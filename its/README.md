# Integration tests (SonarQube plugin)

Integration tests run the Qualimetry Terraform SonarQube plugin against a **live SonarQube server** and verify that analysis produces issues and the rule repository is visible.

Pattern matches the Gherkin plugin: standalone `its` module (not in parent POM), Surefire skipped, Failsafe runs `*IT.java` when profile `its` is active.

## Prerequisites

- **SonarQube** server running with the Qualimetry Terraform plugin JAR installed.
- **sonar-scanner** on PATH, or set `sonar.scanner.bin` to the scanner executable or bin directory (e.g. `C:\sqa\node\scanners\sonar\bin`).
- **tflint**, **trivy**, and **checkov** on PATH (the plugin sensor runs them during analysis).

## Running the integration tests

1. Build and install the plugin into SonarQube:
   ```bash
   mvn clean package -pl terraform-plugin -am
   ```
   Copy `terraform-plugin/target/qualimetry-terraform-plugin-*.jar` to SonarQube `extensions/plugins/` and restart SonarQube.

2. From the **its** directory, run:
   ```bash
   cd its
   mvn verify -Pits -Dsonar.host.url=URL -Dsonar.token=YOUR_TOKEN
   ```
   For UAT with a local scanner bin directory (Windows):
   ```bash
   mvn verify -Pits "-Dsonar.host.url=https://acme.qualimetry.io/uat/sonar/" "-Dsonar.token=YOUR_TOKEN" "-Dsonar.scanner.bin=C:\sqa\node\scanners\sonar\bin"
   ```
   Tokens starting with `sqa_` use Bearer auth. Quote `-D` args on Windows if the URL contains `=`.

3. Use the **Qualimetry Way** (or Qualimetry Terraform) quality profile for the Terraform language on the test projects. Set it as the default Terraform profile or assign it to the ITS project keys.

## Quality gate: noncompliant should fail

The **noncompliant** project is designed to have many issues (Critical/Blocker from Trivy and Checkov, plus tflint). For its quality gate to **fail** (so you can see the plugin enforcing standards), the project must use a quality gate that has a condition on **Overall** code, for example:

- **Blocker issues** greater than 0, or  
- **Critical issues** greater than 0  

The default **Sonar way** gate often only fails on **New code**; for a Terraform-only project with no previous analysis, “New code” may be empty so the gate passes. Do one of the following:

1. **In SonarQube UI:** Quality Gates → Create (e.g. “Terraform ITS”) → Add condition: **Overall code** → **Blocker issues** > **0** (and optionally **Critical issues** > **0**) → Assign the gate to project `terraform-its-noncompliant`.
2. **Script:** Run `scripts/configure-quality-gate-its.ps1` from the repo root to create a **Terraform ITS** quality gate (Overall Blocker > 0, Critical > 0) and assign it to project `terraform-its-noncompliant`. Then re-analyze that project; the gate should fail.

After that, (re-)analyze the noncompliant project; the gate should fail. The **compliant** project should still pass (zero or few issues).

## What the tests do

- **IntegrationTestBase**: Skips tests if the server is not reachable; runs `sonar-scanner` against fixture projects; waits for CE to finish; provisions/deletes projects via the Web API.
- **RuleVerificationIT**:
  - Scans `projects/noncompliant` (Terraform that triggers tflint/trivy/checkov) and `projects/compliant` (minimal Terraform).
  - Asserts the noncompliant project has at least one issue.
  - Asserts the compliant project has zero or very few issues.
  - Asserts the rule repository `qualimetry-terraform` exists and has rules.
  - Asserts at least one issue in the noncompliant project is from a Qualimetry Terraform rule (`qa-tflint-*`, `qa-trivy-*`, `qa-checkov-*`).

## Fixture projects

- **projects/noncompliant** - S3 bucket with public ACL, no encryption/logging; camelCase resource name; no `terraform { required_version }`. Designed to trigger multiple rules.
- **projects/compliant** - Minimal Terraform with `required_version` and a variable only; no resources that trigger security rules.

## Pushing to a staging SonarQube

You can run the scanner manually against the same projects and push to a shared SonarQube (e.g. UAT) using `sonar-scanner` with the appropriate `-Dsonar.host.url` and `-Dsonar.token`. Create the projects first or use the same project keys (`terraform-its-noncompliant`, `terraform-its-compliant`) and ensure the Qualimetry Way (or Qualimetry Terraform) profile is used for Terraform. To have the noncompliant project fail its quality gate, assign a gate that fails when **Overall** Blocker or Critical issues > 0 (see “Quality gate” above).
