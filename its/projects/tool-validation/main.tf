# Tool validation: one or more violations per tool for VS Code plugin testing.
# Open this folder in VS Code, open main.tf, and confirm diagnostics appear for each expected rule below.
#
# EXPECTED VIOLATIONS (see EXPECTED.md in this folder for the full checklist):
#
# --- TFLINT ---
# 1. qa-tflint-terraform_required_version — This file has no "terraform { required_version = \"...\" }" block (typically reported at line 1).
# 2. qa-tflint-terraform_naming_convention — Resource name "myBucket" is camelCase; tflint expects snake_case.
#
# --- TRIVY ---
# 3. qa-trivy-AWS-0086 — S3 bucket missing public access block that blocks public ACLs.
# 4. qa-trivy-AWS-0087 — S3 bucket missing block_public_policy in public access block.
# 5. qa-trivy-AWS-0088 — S3 bucket has no server-side encryption configuration.
# 6. qa-trivy-AWS-0089 — S3 bucket has no server access logging.
#
# --- CHECKOV ---
# 7. qa-checkov-CKV_AWS_20 — S3 bucket ACL is "public-read" (public read access).
# 8. qa-checkov-CKV2_AWS_6 — S3 bucket has no aws_s3_bucket_public_access_block (public access block).

# Intentionally no terraform { required_version } block → tflint terraform_required_version
resource "aws_s3_bucket" "myBucket" {
  bucket = "tool-validation-test-bucket"
  acl    = "public-read" 
}
# No aws_s3_bucket_public_access_block → Trivy AWS-0086, AWS-0087; checkov CKV2_AWS_6
# No server_side_encryption_configuration → Trivy AWS-0088
# No logging block → Trivy AWS-0089
# acl = "public-read" → checkov CKV_AWS_20
