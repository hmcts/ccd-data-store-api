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

output "ELASTIC_SEARCH_HOSTS" {
  value = "${local.elastic_search_hosts}"
}

output "ELASTIC_SEARCH_DATA_NODES_HOSTS" {
  value = "${local.elastic_search_data_node_hosts}"
}

output "DEFINITION_STORE_HOST" {
  value = "${local.definition_store_host}"
}
