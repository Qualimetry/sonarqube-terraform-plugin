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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.opentest4j.TestAbortedException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying that the Qualimetry Terraform plugin correctly
 * detects issues when deployed to a running SonarQube server.
 *
 * <p>Scans two fixture projects:
 * <ul>
 *   <li><strong>noncompliant</strong> - Terraform that triggers tflint/trivy/checkov rules</li>
 *   <li><strong>compliant</strong> - Minimal Terraform that should produce zero or few issues</li>
 * </ul>
 *
 * <p>Prerequisites: SonarQube with the plugin installed; sonar-scanner on PATH;
 * tflint, trivy, and checkov on PATH (sensor runs them).
 *
 * <p>Execution:
 * <pre>
 * cd its
 * mvn verify -Pits -Dsonar.host.url=http://localhost:9000 -Dsonar.token=YOUR_TOKEN
 * </pre>
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RuleVerificationIT extends IntegrationTestBase {

    private static final String NONCOMPLIANT_KEY = "terraform-its-noncompliant";
    private static final String COMPLIANT_KEY = "terraform-its-compliant";

    private static boolean scansCompleted;

    @BeforeAll
    void scanProjects() throws Exception {
        try {
            assumeServerAvailable();
        } catch (TestAbortedException e) {
            scansCompleted = false;
            return;
        }
        provisionProject(NONCOMPLIANT_KEY, "Terraform ITS Noncompliant");
        runScan(Path.of("projects/noncompliant"), NONCOMPLIANT_KEY);
        waitForAnalysisToComplete(NONCOMPLIANT_KEY);

        provisionProject(COMPLIANT_KEY, "Terraform ITS Compliant");
        runScan(Path.of("projects/compliant"), COMPLIANT_KEY);
        waitForAnalysisToComplete(COMPLIANT_KEY);

        scansCompleted = true;
    }

    @Test
    void noncompliantProjectHasIssues() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(scansCompleted, "Scans did not run (server unavailable or @BeforeAll failed)");
        int totalIssues = getIssueCount(NONCOMPLIANT_KEY, null);
        assertThat(totalIssues)
                .as("Noncompliant project should have at least one issue from tflint/trivy/checkov")
                .isGreaterThan(0);
    }

    @Test
    void compliantProjectHasZeroOrFewIssues() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(scansCompleted, "Scans did not run (server unavailable or @BeforeAll failed)");
        int totalIssues = getIssueCount(COMPLIANT_KEY, null);
        assertThat(totalIssues)
                .as("Compliant project should have zero or very few issues")
                .isLessThanOrEqualTo(5);
    }

    @Test
    void ruleRepositoryExistsAndHasRules() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(scansCompleted, "Scans did not run (server unavailable or @BeforeAll failed)");
        JsonObject response = apiGet(
                "/api/rules/search?repositories=" + REPOSITORY_KEY + "&ps=1&p=1");
        int total = response.get("total").getAsInt();
        assertThat(total)
                .as("Rule repository " + REPOSITORY_KEY + " should contain rules")
                .isGreaterThan(0);
    }

    @Test
    void qualimetryTerraformRulesReportedInNoncompliant() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(scansCompleted, "Scans did not run (server unavailable or @BeforeAll failed)");
        String path = "/api/issues/search?componentKeys=" + URLEncoder.encode(NONCOMPLIANT_KEY, StandardCharsets.UTF_8)
                + "&ps=500&p=1";
        JsonObject response = apiGet(path);
        JsonArray issues = response.getAsJsonArray("issues");
        long fromPlugin = 0;
        if (issues != null) {
            for (JsonElement el : issues) {
                String rule = el.getAsJsonObject().get("rule").getAsString();
                if (rule.startsWith(REPOSITORY_KEY + ":qa-")) {
                    fromPlugin++;
                }
            }
        }
        assertThat(fromPlugin)
                .as("At least one issue should be from Qualimetry Terraform rules (qa-tflint-*, qa-trivy-*, qa-checkov-*)")
                .isGreaterThan(0);
    }

    private int getIssueCount(String projectKey, String rule) throws Exception {
        String encodedKey = URLEncoder.encode(projectKey, StandardCharsets.UTF_8);
        StringBuilder path = new StringBuilder("/api/issues/search?componentKeys=");
        path.append(encodedKey);
        path.append("&ps=1&p=1");
        if (rule != null) {
            path.append("&rules=").append(URLEncoder.encode(rule, StandardCharsets.UTF_8));
        }
        JsonObject response = apiGet(path.toString());
        return response.get("total").getAsInt();
    }
}
