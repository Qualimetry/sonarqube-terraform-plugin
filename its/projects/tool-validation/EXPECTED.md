# Expected violations for tool-validation/main.tf

Use this checklist to confirm the VS Code Terraform Analyzer extension reports findings from all three tools (tflint, Trivy, checkov).

**How to test:** Open this folder (`its/projects/tool-validation`) in VS Code/Cursor, then open `main.tf`. Ensure tflint, Trivy, and checkov are on PATH. After a few seconds you should see squiggles and entries in the Problems view.

- **Tflint:** This folder includes a `.tflint.hcl` that enables the Terraform ruleset. If tflint reports no issues, run `tflint --init` once in this folder to install the Terraform plugin, then re-open the file.
- **Output:** Check **View → Output → Terraform Analyzer** for `findings: tflint=X trivy=Y checkov=Z mapped=... for this file=N` to see how many findings each tool returned and how many were published.

---

## Tflint (at least 2 rules)

| Rule ID | Name | What to look for |
|--------|------|-------------------|
| **qa-tflint-terraform_required_version** | Terraform required_version should be specified | No `terraform { required_version = "..." }` block; often reported at line 1. |
| **qa-tflint-terraform_naming_convention** | Terraform naming convention | Resource name `myBucket` is camelCase; tflint expects snake_case (e.g. `my_bucket`). |

---

## Trivy (at least 4 rules)

| Rule ID | Name | What to look for |
|--------|------|-------------------|
| **qa-trivy-AWS-0086** | S3 Access block should block public ACL | Bucket has no `aws_s3_bucket_public_access_block` with `block_public_acls = true`. |
| **qa-trivy-AWS-0087** | S3 Access block should block public policy | Same block missing or without `block_public_policy = true`. |
| **qa-trivy-AWS-0088** | S3 buckets should have encryption enabled | No `aws_s3_bucket_server_side_encryption_configuration`. |
| **qa-trivy-AWS-0089** | S3 buckets should have logging enabled | No server access logging configuration. |

---

## Checkov (at least 2 rules)

| Rule ID | Name | What to look for |
|--------|------|-------------------|
| **qa-checkov-CKV_AWS_20** | S3 Bucket has an ACL defined which allows public READ access | `acl = "public-read"` on the bucket. |
| **qa-checkov-CKV2_AWS_6** | Ensure that S3 bucket has a public access block | No `aws_s3_bucket_public_access_block` resource. |

---

**Summary:** One `.tf` file is written to trigger **at least one rule from each tool**:

- **Tflint:** `terraform_required_version`, `terraform_naming_convention`
- **Trivy:** `AWS-0086`, `AWS-0087`, `AWS-0088`, `AWS-0089`
- **Checkov:** `CKV_AWS_20`, `CKV2_AWS_6`

If any of these do not appear as diagnostics, check **View → Output → Terraform Analyzer** for tool output (e.g. `tflint=ok` / `tflint=fail`).
