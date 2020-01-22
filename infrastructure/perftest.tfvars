idam_api_url = "https://idam-api.perftest.platform.hmcts.net"
frontend_url = "https://www-ccd.perftest.platform.hmcts.net"



capacity = "4"
asp_name = "ccd-data-store-api-perftest"
asp_rg = "ccd-data-store-api-perftest"
elastic_search_enabled = "true"

data_store_max_pool_size = 48
database_sku_name = "GP_Gen5_8"
database_sku_capacity = "8"
database_storage_mb = "52224"

http_client_connection_timeout = 10000
http_client_max_total = 200
#http_client_seconds_idle_connection = 120
http_client_max_client_per_route = 40
#http_client_validate_after_inactivity = 0

definition_cache_latest_version_ttl_sec = 30
