# Enable Terraform ruleset so tflint reports terraform_required_version, terraform_naming_convention, etc.
plugin "terraform" {
  enabled = true
  preset  = "recommended"
}
