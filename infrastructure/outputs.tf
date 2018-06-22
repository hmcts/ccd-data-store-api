output "microserviceName" {
  value = "${local.app_full_name}"
}

output "vaultUri" {
  value = "${local.vaultUri}"
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
