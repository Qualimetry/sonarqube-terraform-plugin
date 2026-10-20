# Intentionally non-compliant Terraform to trigger tflint, Trivy, and checkov rules.
# - No terraform { required_version } -> tflint terraform_required_version
# - Resource name camelCase -> tflint terraform_naming_convention
# - S3: public ACL, no encryption, no logging, public_access_block missing/weak
#   -> Trivy AWS-0086, AWS-0087, AWS-0088, AWS-0089; checkov CKV_AWS_20, CKV2_AWS_6, CKV_AWS_57

resource "aws_s3_bucket" "myBucket" {
  bucket = "test-bucket-validation"

  acl = "public-read"
}

# No block_public_acls, block_public_policy, etc. -> Trivy AWS-0086, AWS-0087
resource "aws_s3_bucket_public_access_block" "example" {
  bucket = aws_s3_bucket.myBucket.id
  # Intentionally omit block_public_acls = true, block_public_policy = true
}

# No server_side_encryption_configuration -> Trivy AWS-0088, checkov
# No logging -> Trivy AWS-0089
