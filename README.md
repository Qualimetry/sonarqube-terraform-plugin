# Qualimetry Terraform Analyzer - SonarQube Plugin

[![CI](https://github.com/Qualimetry/sonarqube-terraform-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/Qualimetry/sonarqube-terraform-plugin/actions/workflows/ci.yml)

**Author**: Qualimetry ([qualimetry.com](https://qualimetry.com)) team at SHAZAM Analytics Ltd

A SonarQube plugin for static analysis of Terraform (HCL) files. It shares the same rule set as the [Qualimetry Terraform Analyzer for VS Code](https://github.com/Qualimetry/vscode-terraform-plugin). The plugin runs **tflint**, **Trivy**, and **checkov** and reports their findings as SonarQube issues with full HTML descriptions, compliant and non-compliant examples, and links to upstream documentation. Reported issues include misconfigurations, security findings, style and convention violations, and correctness errors.

## Requirements

- SonarQube (compatible with the plugin API version in use).
- On the **scanner host** (where the SonarScanner runs): **tflint**, **Trivy**, and **checkov** must be installed and on **PATH**. The plugin runs these tools and reports their findings as SonarQube issues.

Install the tools on the machine that runs the scanner (e.g. CI runner). All three must be on **PATH**:

- **Windows:** **tflint** and **Trivy** via Chocolatey or Scoop (e.g. `choco install tflint trivy`). **checkov** is not on Chocolatey; install with pip: `pip install checkov` (Python 3.9+), then ensure the Scripts folder is on PATH. See [Trivy installation](https://trivy.dev/docs/latest/getting-started/installation/) if needed.
- **Linux:** Official install scripts or distro packages; ensure **tflint**, **Trivy**, and **checkov** are all on PATH.

## Installation

1. Download the plugin JAR from [Releases](https://github.com/Qualimetry/sonarqube-terraform-plugin/releases) (or build from source).
2. Place the JAR in SonarQube’s `extensions/plugins` directory.
3. Restart SonarQube.
4. In **Quality Profiles**, create or use the “Qualimetry Terraform” profile for language **Terraform**.

## Rule count and content

The plugin ships with a rule set derived from tflint, Trivy, and checkov. Each rule has:

- Name, severity, type
- HTML description with non-compliant and compliant examples (or placeholder + doc link)
- “More information” link where available

Rule keys use the **qa-** prefix (e.g. `qa-tflint-*`, `qa-trivy-*`, `qa-checkov-*`). Two built-in profiles: **Qualimetry Terraform** (does not duplicate Sonar Way) and **Qualimetry Way** (full set).

## Build from source

```bash
git clone https://github.com/Qualimetry/sonarqube-terraform-plugin.git
cd sonarqube-terraform-plugin
mvn clean package
# JAR: terraform-plugin/target/qualimetry-terraform-plugin-*.jar
```

Requires JDK 17 and Maven 3.6+.
