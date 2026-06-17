# Feature: VS Code Terraform Analyzer – tool detection
#
# This feature describes the expected diagnostics when the Qualimetry Terraform
# Analyzer extension runs against its/projects/noncompliant/main.tf.
#
# Use it to confirm that all three tools (tflint, Trivy, checkov) produce
# violations that the extension shows as decorations in VS Code/Cursor.
#
# How to verify:
# 1. Open folder: its/projects/noncompliant
# 2. Open file: main.tf
# 3. Check that the extension reports at least the rule listed in each scenario below.
#    (Output → Terraform Analyzer should show tflint=ok, trivy=ok, checkov=ok if tools are on PATH.)

Feature: VS Code plugin detects violations from all three tools

  Scenario: tflint rule – terraform_required_version
    Given the file "its/projects/noncompliant/main.tf" is open in the Terraform Analyzer
    When the extension runs analysis
    Then the extension should report at least one diagnostic for rule "qa-tflint-terraform_required_version"
    And the diagnostic message should refer to "required_version" or "terraform block"
    # Rule: Terraform required_version should be specified.
    # Trigger: main.tf has no terraform { } block with required_version.
    # Expected: Decoration near line 1 (or first line of file).

  Scenario: tflint rule – terraform_naming_convention
    Given the file "its/projects/noncompliant/main.tf" is open in the Terraform Analyzer
    When the extension runs analysis
    Then the extension should report at least one diagnostic for rule "qa-tflint-terraform_naming_convention"
    And the diagnostic should point at a resource name that is not snake_case
    # Rule: Terraform naming convention (default snake_case).
    # Trigger: resource "aws_s3_bucket" "myBucket" uses camelCase "myBucket".
    # Expected: Decoration on the line containing "myBucket".

  Scenario: Trivy rule – S3 public ACL (AWS-0086)
    Given the file "its/projects/noncompliant/main.tf" is open in the Terraform Analyzer
    When the extension runs analysis
    Then the extension should report at least one diagnostic for rule "qa-trivy-AWS-0086"
    And the diagnostic message should refer to "public ACL" or "block_public_acls"
    # Rule: S3 Access block should block public ACL.
    # Trigger: S3 bucket has acl = "public-read" and/or missing block_public_acls in public_access_block.
    # Expected: Decoration on S3 bucket or public_access_block resource.

  Scenario: checkov rule – S3 public READ ACL (CKV_AWS_20)
    Given the file "its/projects/noncompliant/main.tf" is open in the Terraform Analyzer
    When the extension runs analysis
    Then the extension should report at least one diagnostic for rule "qa-checkov-CKV_AWS_20"
    And the diagnostic message should refer to "public" or "ACL" or "READ"
    # Rule: S3 Bucket has an ACL defined which allows public READ access.
    # Trigger: aws_s3_bucket with acl = "public-read".
    # Expected: Decoration on the aws_s3_bucket resource (e.g. line with acl = "public-read").

  Scenario: All three tools contribute diagnostics
    Given the file "its/projects/noncompliant/main.tf" is open in the Terraform Analyzer
    When the extension runs analysis
    Then the extension should report at least one diagnostic from tflint
    And the extension should report at least one diagnostic from Trivy
    And the extension should report at least one diagnostic from checkov
    # Summary: Open main.tf and confirm Problems view / squiggles include:
    # - tflint:     qa-tflint-terraform_required_version, qa-tflint-terraform_naming_convention
    # - Trivy:      qa-trivy-AWS-0086 (and possibly AWS-0087, AWS-0088, AWS-0089)
    # - checkov:    qa-checkov-CKV_AWS_20 (and possibly CKV2_AWS_6, CKV_AWS_57)
