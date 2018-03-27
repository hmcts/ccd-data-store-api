output "vaultUri" {
  value = "${module.ccd-data-store-vault.key_vault_uri}"
}

output "vaultName" {
  value = "${module.ccd-data-store-vault.key_vault_name}"
}
