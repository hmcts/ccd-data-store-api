output "vaultUri" {
  value = "${module.ccd-data-store-vault.key_vault_uri}"
}

output "vaultName" {
  value = "${module.ccd-data-store-vault.key_vault_name}"
}

output "idam_url" {
  value = "${var.idam_api_url}"
}

output "s2s_url" {
  value = "${var.s2s_url}"
}
