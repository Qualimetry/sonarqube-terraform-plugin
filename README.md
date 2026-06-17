# Qualimetry Terraform Analyzer - SonarQube Plugin

[![CI](https://github.com/Qualimetry/sonarqube-terraform-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/Qualimetry/sonarqube-terraform-plugin/actions/workflows/ci.yml)

A SonarQube plugin for static analysis of Terraform files (`.tf`, `.tf.json`) that runs tflint, Trivy, and checkov and reports their findings as SonarQube issues.

Powered by the same analysis engine as the [VS Code extension](https://github.com/Qualimetry/vscode-terraform-plugin) and the [IntelliJ plugin](https://github.com/Qualimetry/intellij-terraform-plugin).

## Features

- **766 rules** from tflint, Trivy, and checkov covering misconfigurations, security, style, and correctness.
- Full HTML descriptions with compliant and non-compliant examples for every rule.
- "More information" links to upstream documentation.
- Two built-in quality profiles: **Qualimetry Terraform** and **Qualimetry Way** (full set).
- Rule keys use the **qa-** prefix (e.g. `qa-tflint-*`, `qa-trivy-*`, `qa-checkov-*`) and align across all three products.

## Rule categories

| Source | Focus | Examples |
|--------|-------|----------|
| tflint | Terraform conventions | Naming conventions, deprecated syntax, provider requirements |
| Trivy | Security misconfigurations | S3 encryption, IAM policies, network security groups |
| checkov | Compliance & best practices | CIS benchmarks, SOC2, HIPAA, PCI-DSS checks |

## Requirements

- SonarQube (compatible with the plugin API version in use).
- On the **scanner host** (where the SonarScanner runs): **tflint**, **Trivy**, and **checkov** must be installed and on **PATH**.

Install the tools on the machine that runs the scanner (e.g. CI runner). All three must be on **PATH**:

- **Windows:** **tflint** and **Trivy** via Chocolatey or Scoop (e.g. `choco install tflint trivy`). **checkov** is not on Chocolatey; install with pip: `pip install checkov` (Python 3.9+), then ensure the Scripts folder is on PATH. See [Trivy installation](https://trivy.dev/docs/latest/getting-started/installation/) if needed.
- **Linux:** Official install scripts or distro packages; ensure **tflint**, **Trivy**, and **checkov** are all on PATH.

## Installation

1. Download the plugin JAR from [Releases](https://github.com/Qualimetry/sonarqube-terraform-plugin/releases) (or build from source).
2. Place the JAR in SonarQube's `extensions/plugins` directory.
3. Restart SonarQube.
4. In **Quality Profiles**, create or use the "Qualimetry Terraform" profile for language **Terraform**.

## Also available

The same analysis engine powers editor extensions for real-time feedback:

- **[VS Code extension](https://github.com/Qualimetry/vscode-terraform-plugin)** — catch issues as you type, before you commit.
- **[IntelliJ plugin](https://github.com/Qualimetry/intellij-terraform-plugin)** — real-time analysis in JetBrains IDEs and Qodana CI/CD.

Rule keys and severities align across all three tools so findings are directly comparable.

## Build from source

```bash
git clone https://github.com/Qualimetry/sonarqube-terraform-plugin.git
cd sonarqube-terraform-plugin
mvn clean package
# JAR: terraform-plugin/target/qualimetry-terraform-plugin-*.jar
```

Requires JDK 17 and Maven 3.6+.
