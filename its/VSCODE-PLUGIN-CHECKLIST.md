# VS Code plugin detection checklist

Use **its/projects/noncompliant/main.tf** and the Qualimetry Terraform Analyzer extension.  
Open the folder, then open `main.tf`. Ensure tflint, Trivy, and checkov are on PATH (or set in settings).

## Rules to verify (at least one per tool)

### 1. tflint

| Rule key | What to look for | Where in main.tf |
|----------|------------------|------------------|
| **qa-tflint-terraform_required_version** | "required_version" / terraform block | Line 1 (or start of file); no `terraform { required_version = "..." }` |
| **qa-tflint-terraform_naming_convention** | Naming convention / snake_case | Line 7: `resource "aws_s3_bucket" "myBucket"` — `myBucket` is camelCase |

### 2. Trivy

| Rule key | What to look for | Where in main.tf |
|----------|------------------|------------------|
| **qa-trivy-AWS-0086** | Block public ACL / block_public_acls | S3 bucket or `aws_s3_bucket_public_access_block` (lines 7–16) |
| **qa-trivy-AWS-0087** | Block public policy | Same block |
| **qa-trivy-AWS-0088** | Encryption enabled | S3 bucket (no server_side_encryption_configuration) |
| **qa-trivy-AWS-0089** | Logging enabled | S3 bucket (no logging block) |

### 3. checkov

| Rule key | What to look for | Where in main.tf |
|----------|------------------|------------------|
| **qa-checkov-CKV_AWS_20** | Public READ ACL | Line 10: `acl = "public-read"` |
| **qa-checkov-CKV2_AWS_6** | S3 public access block | Bucket has no/incomplete public_access_block |
| **qa-checkov-CKV_AWS_57** | Public WRITE ACL | Related to public ACL configuration |

---

**Quick check:** In the **Problems** view (or squiggles in the editor), confirm you see at least:

- One **tflint** rule (e.g. `qa-tflint-terraform_required_version` at line 1).
- One **Trivy** rule (e.g. `qa-trivy-AWS-0086`).
- One **checkov** rule (e.g. `qa-checkov-CKV_AWS_20`).

If any tool shows no diagnostics, check **View → Output → Terraform Analyzer** for `tflint=ok`, `trivy=ok`, `checkov=ok`. A `fail` means that tool is not on PATH or failed to run.
