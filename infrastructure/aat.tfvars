#idam_api_url = "https://preprod-idamapi.reform.hmcts.net:3511"
idam_api_url = "https://idam-api.aat.platform.hmcts.net"
document_management_valid_domain = "^https?://(?:api-gateway\\.preprod\\.dm\\.reform\\.hmcts\\.net|dm-store-aat\\.service\\.core-compute-aat\\.internal(?::\\d+)?)"
default_print_url = "https://return-case-doc-ccd.nonprod.platform.hmcts.net/jurisdictions/:jid/case-types/:ctid/cases/:cid"
capacity = "4"
frontend_url = "https://www-ccd.nonprod.platform.hmcts.net"
asp_name = "ccd-data-store-api-aat"
asp_rg = "ccd-data-store-api-aat"
elastic_search_enabled = "true"

data_store_max_pool_size = 48
database_sku_name = "GP_Gen5_8"
database_sku_capacity = "8"

http_client_read_timeout = "180000"

definition_cache_latest_version_ttl_sec = 1