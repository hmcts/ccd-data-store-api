idam_api_url = "https://idam-api.platform.hmcts.net"
document_management_valid_domain = "^https?://(?:api-gateway\\.dm\\.reform\\.hmcts\\.net|dm-store-prod\\.service\\.core-compute-prod\\.internal(?::\\d+)?)"
default_print_url = "https://return-case-doc.ccd.platform.hmcts.net/jurisdictions/:jid/case-types/:ctid/cases/:cid"
frontend_url = "https://www.ccd.platform.hmcts.net"

capacity = "4"
asp_name = "ccd-data-store-api-prod"
asp_rg = "ccd-data-store-api-prod"
elastic_search_enabled = "true"

data_store_max_pool_size = 48
database_sku_name = "GP_Gen5_8"
database_sku_capacity = "8"

http_client_connection_timeout = 10000
http_client_max_total = 200
#http_client_seconds_idle_connection = 120
http_client_max_client_per_route = 40
#http_client_validate_after_inactivity = 0

definition_cache_latest_version_ttl_sec = 30
definition_cache_jurisdiction_ttl_sec = 120
