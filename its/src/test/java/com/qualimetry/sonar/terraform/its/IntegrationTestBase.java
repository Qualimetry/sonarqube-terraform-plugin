/*
 * Copyright 2026 SHAZAM Analytics Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qualimetry.sonar.terraform.its;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assumptions;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Base class for Terraform plugin integration tests against a running SonarQube server.
 *
 * <p>Provides helper methods for:
 * <ul>
 *   <li>Checking server availability (tests are skipped if unavailable)</li>
 *   <li>Running sonar-scanner against fixture projects</li>
 *   <li>Polling the Compute Engine until analysis completes</li>
 *   <li>Provisioning and deleting projects via the Web API</li>
 *   <li>Making authenticated GET/POST requests to the SonarQube REST API</li>
 * </ul>
 *
 * <h3>System Properties</h3>
 * <dl>
 *   <dt>{@code sonar.host.url}</dt><dd>Base URL of the SonarQube server (default: http://localhost:9000)</dd>
 *   <dt>{@code sonar.token}</dt><dd>Authentication token for the SonarQube API (use Bearer for {@code sqa_*} tokens)</dd>
 *   <dt>{@code sonar.scanner.bin}</dt><dd>Optional: full path to sonar-scanner executable, or scanner bin directory (e.g. C:\sqa\node\scanners\sonar\bin). If unset, uses sonar-scanner from PATH.</dd>
 * </dl>
 *
 * <h3>Prerequisites</h3>
 * <ul>
 *   <li>SonarQube server running with the Qualimetry Terraform plugin installed</li>
 *   <li>{@code sonar-scanner} on PATH or {@code sonar.scanner.bin} set to scanner executable or bin directory</li>
 *   <li>tflint, trivy, and checkov on PATH (sensor runs them during analysis)</li>
 * </ul>
 */
abstract class IntegrationTestBase {

    protected static final String SONAR_URL =
            System.getProperty("sonar.host.url", "http://localhost:9000");

    protected static final String SONAR_TOKEN =
            System.getProperty("sonar.token", "");

    protected static final String REPOSITORY_KEY = "qualimetry-terraform";

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final int CE_POLL_MAX_ATTEMPTS = 60;
    private static final long CE_POLL_INTERVAL_MS = 2000;
    private static final long CE_FALLBACK_WAIT_MS = 15_000;
    private static final long SCANNER_TIMEOUT_MINUTES = 5;

    /**
     * Assumes a SonarQube server is reachable and UP. Skips the test otherwise.
     */
    protected static void assumeServerAvailable() {
        try {
            HttpRequest request = newGetRequest("/api/system/status");
            HttpResponse<String> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString());
            Assumptions.assumeTrue(response.statusCode() == 200,
                    "SonarQube server not available at " + SONAR_URL + " (status=" + response.statusCode() + ")");
            String status = "?";
            try {
                JsonElement el = GSON.fromJson(response.body(), JsonElement.class);
                if (el != null && el.isJsonObject() && el.getAsJsonObject().has("status")) {
                    status = el.getAsJsonObject().get("status").getAsString();
                }
            } catch (Exception e) {
                // Some proxies or versions return non-JSON. Treat 200 as reachable.
            }
            Assumptions.assumeTrue("UP".equals(status) || "?".equals(status),
                    "SonarQube server is not in UP status (got: " + status + ")");
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Cannot connect to SonarQube server at " + SONAR_URL + ": " + e.getMessage());
        }
    }

    /**
     * Executes sonar-scanner against the given project directory. If {@code sonar.scanner.bin} is set,
     * uses that path (executable or bin directory, e.g. C:\sqa\node\scanners\sonar\bin). Otherwise uses sonar-scanner from PATH.
     */
    protected static void runScan(Path projectDir, String projectKey)
            throws IOException, InterruptedException {
        String absoluteBase = projectDir.toAbsolutePath().toString();
        String scannerExe = resolveScannerCommand();
        String scanTool = scannerExe.contains(File.separator) ? "sonar-scanner (" + scannerExe + ")" : "sonar-scanner";
        ProcessBuilder pb = new ProcessBuilder(
                scannerExe,
                "-Dsonar.projectKey=" + projectKey,
                "-Dsonar.host.url=" + SONAR_URL,
                "-Dsonar.token=" + SONAR_TOKEN,
                "-Dsonar.projectBaseDir=" + absoluteBase,
                "-Dsonar.inclusions=**/*.tf,**/*.tf.json"
        );
        pb.directory(projectDir.toFile());
        pb.inheritIO();
        Process process = pb.start();
        boolean finished = process.waitFor(SCANNER_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException(
                    scanTool + " timed out after " + SCANNER_TIMEOUT_MINUTES + " minutes");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException(
                    scanTool + " exited with code " + process.exitValue());
        }
    }

    /**
     * Waits for the Compute Engine analysis to complete for the given project.
     */
    protected static void waitForAnalysisToComplete(String projectKey) throws Exception {
        String encodedKey = URLEncoder.encode(projectKey, StandardCharsets.UTF_8);
        String path = "/api/ce/activity?component=" + encodedKey + "&status=PENDING,IN_PROGRESS";
        for (int attempt = 0; attempt < CE_POLL_MAX_ATTEMPTS; attempt++) {
            try {
                JsonObject response = apiGet(path);
                JsonArray tasks = response.getAsJsonArray("tasks");
                if (tasks == null || tasks.isEmpty()) {
                    return;
                }
            } catch (RuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("403") || msg.contains("non-JSON") || msg.contains("not an object")
                        || msg.contains("JsonPrimitive") || msg.contains("JsonSyntax")) {
                    Thread.sleep(CE_FALLBACK_WAIT_MS);
                    return;
                }
                throw e;
            }
            Thread.sleep(CE_POLL_INTERVAL_MS);
        }
        throw new RuntimeException(
                "Analysis did not complete within "
                        + (CE_POLL_MAX_ATTEMPTS * CE_POLL_INTERVAL_MS / 1000)
                        + " seconds for project: " + projectKey);
    }

    protected static void provisionProject(String projectKey, String name) throws Exception {
        String body = "project=" + URLEncoder.encode(projectKey, StandardCharsets.UTF_8)
                + "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        HttpRequest request = newPostRequest("/api/projects/create", body);
        HttpResponse<String> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 400) {
            throw new RuntimeException(
                    "Failed to provision project '" + projectKey + "': "
                            + response.statusCode() + " " + response.body());
        }
    }

    protected static void deleteProject(String projectKey) {
        try {
            String body = "project=" + URLEncoder.encode(projectKey, StandardCharsets.UTF_8);
            HttpRequest request = newPostRequest("/api/projects/delete", body);
            HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // Ignore
        }
    }

    protected static JsonObject apiGet(String path) throws Exception {
        HttpRequest request = newGetRequest(path);
        HttpResponse<String> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "API GET " + path + " returned " + response.statusCode()
                            + ": " + response.body());
        }
        String body = response.body();
        if (body != null && body.trim().toLowerCase().startsWith("<")) {
            throw new RuntimeException("API GET " + path + " returned non-JSON (e.g. HTML), status=200.");
        }
        try {
            JsonElement el = GSON.fromJson(body, JsonElement.class);
            if (el == null || !el.isJsonObject()) {
                throw new RuntimeException("API GET " + path + " returned JSON but not an object.");
            }
            return el.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("API GET " + path + " returned non-JSON (parse error). " + e.getMessage());
        }
    }

    protected static JsonObject apiPost(String path, String formBody) throws Exception {
        HttpRequest request = newPostRequest(path, formBody);
        HttpResponse<String> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "API POST " + path + " returned " + response.statusCode()
                            + ": " + response.body());
        }
        return GSON.fromJson(response.body(), JsonObject.class);
    }

    private static HttpRequest newGetRequest(String path) {
        String uri = SONAR_URL.endsWith("/") && path.startsWith("/") ? SONAR_URL + path.substring(1) : SONAR_URL + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET();
        addAuth(builder);
        return builder.build();
    }

    private static HttpRequest newPostRequest(String path, String formBody) {
        String uri = SONAR_URL.endsWith("/") && path.startsWith("/") ? SONAR_URL + path.substring(1) : SONAR_URL + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(formBody));
        addAuth(builder);
        return builder.build();
    }

    private static void addAuth(HttpRequest.Builder builder) {
        if (SONAR_TOKEN != null && !SONAR_TOKEN.isBlank()) {
            if (SONAR_TOKEN.startsWith("sqa_")) {
                builder.header("Authorization", "Bearer " + SONAR_TOKEN);
            } else {
                String credentials = SONAR_TOKEN + ":";
                String encoded = Base64.getEncoder()
                        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + encoded);
            }
        }
    }

    /** Resolves scanner executable: sonar.scanner.bin (path or dir) or sonar-scanner from PATH. */
    private static String resolveScannerCommand() {
        String bin = System.getProperty("sonar.scanner.bin", "").trim();
        if (bin.isEmpty()) {
            return getScannerCommand();
        }
        File f = new File(bin);
        if (f.isFile()) {
            return bin;
        }
        if (f.isDirectory()) {
            String os = System.getProperty("os.name", "").toLowerCase();
            String exe = os.contains("win") ? "sonar-scanner.bat" : "sonar-scanner";
            return new File(f, exe).getAbsolutePath();
        }
        return bin;
    }

    private static String getScannerCommand() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "sonar-scanner.bat" : "sonar-scanner";
    }
}
