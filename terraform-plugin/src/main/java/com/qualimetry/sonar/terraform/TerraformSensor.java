package com.qualimetry.sonar.terraform;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.qualimetry.terraform.rules.CheckovOutputParser;
import com.qualimetry.terraform.rules.MappedFinding;
import com.qualimetry.terraform.rules.RuleRegistry;
import com.qualimetry.terraform.rules.TflintOutputParser;
import com.qualimetry.terraform.rules.ToolFinding;
import com.qualimetry.terraform.rules.ToolResultMapper;
import com.qualimetry.terraform.rules.TrivyOutputParser;

/**
 * Runs tflint, Trivy config, and checkov from the project base directory, parses their JSON output,
 * and reports issues to SonarQube. Requires tflint, trivy, and checkov on PATH.
 */
public class TerraformSensor implements Sensor {

    private static final Logger LOG = Loggers.get(TerraformSensor.class);
    private static final int PROCESS_TIMEOUT_SEC = 120;

    private final RuleRegistry registry = new RuleRegistry();
    private final ToolResultMapper mapper = new ToolResultMapper(registry);

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("Terraform (tflint, trivy, checkov)");
        descriptor.onlyOnLanguage(TerraformRulesDefinition.LANGUAGE);
    }

    @Override
    public void execute(SensorContext context) {
        if (!context.fileSystem().hasFiles(context.fileSystem().predicates().hasLanguage(TerraformRulesDefinition.LANGUAGE))) {
            return;
        }
        File baseDir = context.fileSystem().baseDir();
        String basePath = baseDir.getAbsolutePath();

        List<MappedFinding> allFindings = new ArrayList<>();
        boolean ranTflint = runTflint(basePath, allFindings);
        boolean ranTrivy = runTrivy(basePath, allFindings);
        boolean ranCheckov = runCheckov(basePath, allFindings);
        if (!ranTflint && !ranTrivy && !ranCheckov) {
            LOG.warn("Terraform analysis skipped: tflint, trivy, and checkov could not be run. Install them on the scanner host PATH. See the plugin README.");
        }

        for (MappedFinding f : allFindings) {
            org.sonar.api.batch.fs.InputFile inputFile = context.fileSystem().inputFile(
                    context.fileSystem().predicates().hasRelativePath(f.getFile()));
            if (inputFile != null) {
                NewIssue newIssue = context.newIssue()
                        .forRule(RuleKey.of(TerraformRulesDefinition.REPOSITORY_KEY, f.getRuleKey()));
                NewIssueLocation loc = newIssue.newLocation()
                        .on(inputFile)
                        .at(inputFile.selectLine(f.getLine()))
                        .message(f.getMessage());
                newIssue.at(loc);
                newIssue.save();
            }
        }
    }

    private boolean runTflint(String basePath, List<MappedFinding> out) {
        try {
            String json = runProcess(basePath, "tflint", "--format", "json");
            if (json == null) {
                LOG.warn("Tflint could not be run (not on PATH or failed). Install it on the scanner host. See the plugin README.");
                return false;
            }
            List<ToolFinding> findings = TflintOutputParser.parse(json, basePath);
            out.addAll(mapper.mapTflint(findings));
            return true;
        } catch (Exception e) {
            LOG.warn("Tflint failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean runTrivy(String basePath, List<MappedFinding> out) {
        try {
            String json = runProcess(basePath, "trivy", "config", "-f", "json", ".");
            if (json == null) {
                LOG.warn("Trivy could not be run (not on PATH or failed). Install it on the scanner host. See the plugin README.");
                return false;
            }
            List<ToolFinding> findings = TrivyOutputParser.parse(json, basePath);
            out.addAll(mapper.mapTrivy(findings));
            return true;
        } catch (Exception e) {
            LOG.warn("Trivy failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean runCheckov(String basePath, List<MappedFinding> out) {
        try {
            String json = runProcess(basePath, "checkov", "-d", ".", "--output", "json");
            if (json == null) {
                LOG.warn("Checkov could not be run (not on PATH or failed). Install it on the scanner host. See the plugin README.");
                return false;
            }
            List<ToolFinding> findings = CheckovOutputParser.parse(json, basePath);
            out.addAll(mapper.mapCheckov(findings));
            return true;
        } catch (Exception e) {
            LOG.warn("Checkov failed: {}", e.getMessage());
            return false;
        }
    }

    private static String runProcess(String workDir, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command)
                    .directory(new File(workDir))
                    .redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            if (!p.waitFor(PROCESS_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
