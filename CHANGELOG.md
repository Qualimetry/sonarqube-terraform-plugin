# Changelog

## [Unreleased]

- (None.)

## [2.2.12] - 2026-10-21

- 73 new rules from Trivy's Azure checks (`qa-trivy-AZU-*`): Trivy findings on Azure Terraform resources (App Service, Storage accounts, Key Vault, SQL and PostgreSQL, AKS, network security groups, Defender for Cloud and more) are now reported instead of being dropped. Active in Qualimetry Way, Qualimetry Azure Way and Qualimetry Terraform (`qa-trivy-AZU-0036`, unencrypted Data Lake storage, is left out of Qualimetry Terraform because Sonar way already covers it).
- Qualimetry AWS Way now activates only the Trivy AWS rules (`qa-trivy-AWS-*`) and Qualimetry Azure Way only the Trivy Azure rules (`qa-trivy-AZU-*`).

## [2.2.11] - 2026-10-20

- 19 new rules from the checkov secrets scanner (`qa-checkov-CKV_SECRET_1` to `qa-checkov-CKV_SECRET_19`): hard-coded passwords, access keys, tokens and other high-entropy literals in Terraform files are reported as Vulnerabilities. Active in Qualimetry Way, Qualimetry AWS Way and Qualimetry Azure Way.
- checkov findings are no longer lost when checkov reports more than one framework in one run (for example Terraform plus secrets).
- tflint now runs recursively, so Terraform sub-modules are analysed as well as the root module.
- Tool output that starts with log or banner lines is parsed correctly instead of being discarded.
- On Windows, a checkov launcher script (`checkov.cmd`) on PATH is found and run.

## [2.2.10] - 2026-09-06

- Version-alignment release.

## [2.2.9] - 2026-06-23

- Renamed the rules repository to "Qualimetry" so SonarQube no longer shows the language label twice.

## [2.2.8] - 2026-06-17

- Added full-text, per-rule documentation pages for every rule.

## [2.2.1] - 2026-02-28

First public release.

- Static analysis of Terraform (HCL) files in SonarQube via tflint, Trivy, and checkov.
- Rule repository **qualimetry-terraform** with quality profiles (Qualimetry Terraform, Qualimetry AWS Way, Qualimetry Azure Way).
- 766 rules with full HTML descriptions covering security, best practices, and compliance.
