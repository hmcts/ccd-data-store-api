output "microserviceName" {
  value = "${local.app_full_name}"
}

output "vaultUri" {
  value = "${data.azurerm_key_vault.ccd_shared_key_vault.vault_uri}"
}

output "vaultName" {
  value = "${local.vaultName}"
}

output "idam_url" {
  value = "${var.idam_api_url}"
}

output "s2s_url" {
  value = "${local.s2s_url}"
}

output "CCD_GW_SERVICE_NAME" {
  value = "ccd_gw"
}

output "OAUTH2_CLIENT_ID" {
  value = "ccd_gateway"
}

output "OAUTH2_REDIRECT_URI" {
  value = "${local.oauth2_redirect_uri}"
}

output "ELASTIC_SEARCH_ENABLED" {
  value = "${var.elastic_search_enabled}"
}

output "ELASTIC_SEARCH_HOST" {
  value = "${local.elastic_search_host}"
}

output "ELASTIC_SEARCH_PORT" {
  value = "${var.elastic_search_port}"
}

output "ELASTIC_SEARCH_SCHEME" {
  value = "${var.elastic_search_scheme}"
}
