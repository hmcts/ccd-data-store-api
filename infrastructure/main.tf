provider "azurerm" {
  version = "1.22.1"
}

locals {
  app_full_name = "${var.product}-${var.component}"

  aseName = "core-compute-${var.env}"
  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"
  local_ase = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "core-compute-aat" : "core-compute-saat" : local.aseName}"
  env_ase_url = "${local.local_env}.service.${local.local_ase}.internal"

  default_default_print_url = "https://ccd-case-print-service-${local.env_ase_url}/jurisdictions/:jid/case-types/:ctid/cases/:cid"
  default_print_url = "${var.default_print_url != "" ? var.default_print_url : local.default_default_print_url}"

  default_dm_valid_domain = "^https?://(?:api-gateway\\.test\\.dm\\.reform\\.hmcts\\.net|dm-store-${local.local_env}\\.service\\.${local.local_ase}\\.internal(?::\\d+)?)"
  dm_valid_domain = "${var.document_management_valid_domain != "" ? var.document_management_valid_domain : local.default_dm_valid_domain}"

  // Vault name
  previewVaultName = "${var.raw_product}-aat"
  nonPreviewVaultName = "${var.raw_product}-${var.env}"
  vaultName = "${(var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName}"

  // Shared Resource Group
  previewResourceGroup = "${var.raw_product}-shared-aat"
  nonPreviewResourceGroup = "${var.raw_product}-shared-${var.env}"
  sharedResourceGroup = "${(var.env == "preview" || var.env == "spreview") ? local.previewResourceGroup : local.nonPreviewResourceGroup}"

  sharedAppServicePlan = "${var.raw_product}-${var.env}"
  sharedASPResourceGroup = "${var.raw_product}-shared-${var.env}"

  // S2S
  s2s_url = "http://rpe-service-auth-provider-${local.env_ase_url}"

  custom_redirect_uri = "${var.frontend_url}/oauth2redirect"
  default_redirect_uri = "https://ccd-case-management-web-${local.env_ase_url}/oauth2redirect"
  oauth2_redirect_uri = "${var.frontend_url != "" ? local.custom_redirect_uri : local.default_redirect_uri}"

  draftStoreUrl = "http://draft-store-service-${local.local_env}.service.${local.local_ase}.internal"
  elastic_search_hosts = "${var.elastic_search_enabled == "false" ? "" : "${format("http://%s:9200", join("", data.azurerm_key_vault_secret.ccd_elastic_search_url.*.value))}"}"
  elastic_search_data_node_hosts = "${var.elastic_search_enabled == "false" ? "" : "${join("", data.azurerm_key_vault_secret.ccd_elastic_search_data_nodes_url.*.value)}"}"
  elastic_search_password = "${var.elastic_search_enabled == "false" ? "" : "${join("", data.azurerm_key_vault_secret.ccd_elastic_search_password.*.value)}"}"
  definition_store_host = "http://ccd-definition-store-api-${local.env_ase_url}"
}

data "azurerm_key_vault" "ccd_shared_key_vault" {
  name = "${local.vaultName}"
  resource_group_name = "${local.sharedResourceGroup}"
}

data "azurerm_key_vault" "s2s_vault" {
  name = "s2s-${local.local_env}"
  resource_group_name = "rpe-service-auth-provider-${local.local_env}"
}

data "azurerm_key_vault_secret" "ccd_data_s2s_key" {
  name = "microservicekey-ccd-data"
  key_vault_id = "${data.azurerm_key_vault.s2s_vault.id}"
}

resource "azurerm_key_vault_secret" "ccd_data_s2s_secret" {
  name = "ccd-data-s2s-secret"
  value = "${data.azurerm_key_vault_secret.ccd_data_s2s_key.value}"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}

// load balancer url. The load balancer will kill connections when idle for 5 min. Used by functional tests
data "azurerm_key_vault_secret" "ccd_elastic_search_url" {
  count = "${var.elastic_search_enabled == "false" ? 0 : 1}"
  name = "ccd-ELASTIC-SEARCH-URL"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}

// format: "http://ccd-data-1:9200","http://ccd-data-2:9200"
data "azurerm_key_vault_secret" "ccd_elastic_search_data_nodes_url" {
  count = "${var.elastic_search_enabled == "false" ? 0 : 1}"
  name = "ccd-ELASTIC-SEARCH-DATA-NODES-URL"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}

data "azurerm_key_vault_secret" "ccd_elastic_search_password" {
  count = "${var.elastic_search_enabled == "false" ? 0 : 1}"
  name = "ccd-ELASTIC-SEARCH-PASSWORD"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}

resource "random_string" "draft_encryption_key" {
  length  = 16
  special = true
  upper   = true
  lower   = true
  number  = true
  lifecycle {
    ignore_changes = ["*"]
  }
}

module "ccd-data-store-api" {
  source   = "git@github.com:hmcts/cnp-module-webapp?ref=master"
  product  = "${local.app_full_name}"
  location = "${var.location}"
  appinsights_location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"
  subscription = "${var.subscription}"
  is_frontend = false
  common_tags  = "${var.common_tags}"
  additional_host_name = "${var.additional_host_name}"
  asp_name = "${(var.asp_name == "use_shared") ? local.sharedAppServicePlan : var.asp_name}"
  asp_rg = "${(var.asp_rg == "use_shared") ? local.sharedASPResourceGroup : var.asp_rg}"
  website_local_cache_sizeinmb = 2000
  capacity = "${var.capacity}"
  java_container_version = "9.0"
  appinsights_instrumentation_key = "${var.appinsights_instrumentation_key}"
  enable_ase                      = "${var.enable_ase}"

  app_settings = {
    DATA_STORE_DB_HOST = "${module.data-store-db.host_name}"
    DATA_STORE_DB_PORT = "${module.data-store-db.postgresql_listen_port}"
    DATA_STORE_DB_NAME = "${module.data-store-db.postgresql_database}"
    DATA_STORE_DB_USERNAME = "${module.data-store-db.user_name}"
    DATA_STORE_DB_PASSWORD = "${module.data-store-db.postgresql_password}"
    DATA_STORE_DB_MAX_POOL_SIZE = "${var.data_store_max_pool_size}"
    DATA_STORE_DB_OPTIONS = "?stringtype=unspecified&sslmode=require"

    ENABLE_DB_MIGRATE = "false"

    DEFINITION_STORE_HOST               = "${local.definition_store_host}"
    USER_PROFILE_HOST                   = "http://ccd-user-profile-api-${local.env_ase_url}"

    CCD_DM_DOMAIN                       = "${local.dm_valid_domain}"

    IDAM_USER_URL                       = "${var.idam_api_url}"
    IDAM_S2S_URL                        = "${local.s2s_url}"
    DATA_STORE_IDAM_KEY                 = "${data.azurerm_key_vault_secret.ccd_data_s2s_key.value}"

    CCD_DRAFT_STORE_URL                 = "${local.draftStoreUrl}"
    CCD_DRAFT_TTL_DAYS                  = "${var.draft_store_ttl_days}"
    CCD_DRAFT_ENCRYPTION_KEY            = "${random_string.draft_encryption_key.result}"

    DATA_STORE_S2S_AUTHORISED_SERVICES  = "${var.authorised-services}"

    CCD_DEFAULTPRINTURL                 = "${local.default_print_url}"

    DEFINITION_CACHE_MAX_IDLE_SEC       = "${var.definition_cache_max_idle_sec}"
    DEFINITION_CACHE_LATEST_VERSION_TTL_SEC = "${var.definition_cache_latest_version_ttl_sec}"
    DEFINITION_CACHE_MAX_SIZE           = "${var.definition_cache_max_size}"
    DEFINITION_CACHE_EVICTION_POLICY    = "${var.definition_cache_eviction_policy}"

    ELASTIC_SEARCH_ENABLED              = "${var.elastic_search_enabled}"
    ELASTIC_SEARCH_HOSTS                = "${local.elastic_search_hosts}"
    ELASTIC_SEARCH_DATA_NODES_HOSTS     = "${local.elastic_search_data_node_hosts}"
    ELASTIC_SEARCH_PASSWORD             = "${local.elastic_search_password}"
    ELASTIC_SEARCH_BLACKLIST            = "${var.elastic_search_blacklist}"
    ELASTIC_SEARCH_CASE_INDEX_NAME_FORMAT = "${var.elastic_search_case_index_name_format}"
    ELASTIC_SEARCH_CASE_INDEX_TYPE      = "${var.elastic_search_case_index_type}"
    ELASTIC_SEARCH_NODES_DISCOVERY_ENABLED = "${var.elastic_search_nodes_discovery_enabled}"
    ELASTIC_SEARCH_NODES_DISCOVERY_FREQUENCY_MILLIS = "${var.elastic_search_nodes_discovery_frequency_millis}"
    ELASTIC_SEARCH_NODES_DISCOVERY_FILTER = "${var.elastic_search_nodes_discovery_filter}"
    ELASTIC_SEARCH_REQUEST_TIMEOUT        = "${var.elastic_search_request_timeout}"

    HTTP_CLIENT_CONNECTION_TIMEOUT        = "${var.http_client_connection_timeout}"
    HTTP_CLIENT_READ_TIMEOUT              = "${var.http_client_read_timeout}"
    HTTP_CLIENT_MAX_TOTAL                 = "${var.http_client_max_total}"
    HTTP_CLIENT_SECONDS_IDLE_CONNECTION   = "${var.http_client_seconds_idle_connection}"
    HTTP_CLIENT_MAX_CLIENT_PER_ROUTE      = "${var.http_client_max_client_per_route}"
    HTTP_CLIENT_VALIDATE_AFTER_INACTIVITY = "${var.http_client_validate_after_inactivity}"

    CCD_AM_WRITE_TO_CCD_ONLY              = "${var.ccd_am_write_to_ccd_only}"
    CCD_AM_WRITE_TO_AM_ONLY               = "${var.ccd_am_write_to_am_only}"
    CCD_AM_WRITE_TO_BOTH                  = "${var.ccd_am_write_to_both}"
    CCD_AM_READ_FROM_CCD                  = "${var.ccd_am_read_from_ccd}"
    CCD_AM_READ_FROM_AM                   = "${var.ccd_am_read_from_am}"
    JPA_CRITERIA_IN_SEARCH_ENABLED        = false
  }

}

module "data-store-db" {
  source = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product = "${local.app_full_name}-postgres-db"
  location = "${var.location}"
  env = "${var.env}"
  subscription = "${var.subscription}"
  postgresql_user = "${var.postgresql_user}"
  database_name = "${var.database_name}"
  sku_name = "${var.database_sku_name}"
  sku_tier = "GeneralPurpose"
  sku_capacity = "${var.database_sku_capacity}"
  storage_mb = "${var.database_storage_mb}"
  common_tags  = "${var.common_tags}"
}

////////////////////////////////
// Populate Vault with DB info
////////////////////////////////

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name = "${var.component}-POSTGRES-USER"
  value = "${module.data-store-db.user_name}"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name = "${var.component}-POSTGRES-PASS"
  value = "${module.data-store-db.postgresql_password}"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name = "${var.component}-POSTGRES-HOST"
  value = "${module.data-store-db.host_name}"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name = "${var.component}-POSTGRES-PORT"
  value = "${module.data-store-db.postgresql_listen_port}"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name = "${var.component}-POSTGRES-DATABASE"
  value = "${module.data-store-db.postgresql_database}"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}

resource "azurerm_key_vault_secret" "ccd_draft_encryption_key" {
  name = "${var.component}-draftStoreEncryptionSecret"
  value = "${random_string.draft_encryption_key.result}"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}

resource "azurerm_key_vault_secret" "draft-store-key" {
  name = "${var.component}-draft-key"
  value = "${random_string.draft_encryption_key.result}"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}
