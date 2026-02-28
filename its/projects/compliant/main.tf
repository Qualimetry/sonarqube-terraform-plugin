# Minimal compliant Terraform for ITS: required_version set, snake_case name.
# No S3/security resources to avoid tfsec/checkov findings.
terraform {
  required_version = ">= 1.0"
}

variable "example" {
  type    = string
  default = "value"
}
