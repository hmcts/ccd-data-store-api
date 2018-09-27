locals {
  app_full_name = "${var.product}-${var.component}"

  aseName = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"
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
}

data "azurerm_key_vault" "ccd_shared_key_vault" {
  name = "${local.vaultName}"
  resource_group_name = "${local.sharedResourceGroup}"
}

data "azurerm_key_vault_secret" "ccd_data_s2s_key" {
  name = "ccd-data-store-api-s2s-secret"
  vault_uri = "${data.azurerm_key_vault.ccd_shared_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "ccd_elastic_search_url" {
  name = "ccd-ELASTIC-SEARCH-URL"
  vault_uri = "${data.azurerm_key_vault.ccd_shared_key_vault.vault_uri}"
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
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"
  subscription = "${var.subscription}"
  is_frontend = false
  common_tags  = "${var.common_tags}"
  additional_host_name = "debugparam"
  asp_name = "${(var.asp_name == "use_shared") ? local.sharedAppServicePlan : var.asp_name}"
  asp_rg = "${(var.asp_rg == "use_shared") ? local.sharedASPResourceGroup : var.asp_rg}"
  website_local_cache_sizeinmb = 1050

  app_settings = {
    DATA_STORE_DB_HOST = "${module.data-store-db.host_name}"
    DATA_STORE_DB_PORT = "${module.data-store-db.postgresql_listen_port}"
    DATA_STORE_DB_NAME = "${module.data-store-db.postgresql_database}"
    DATA_STORE_DB_USERNAME = "${module.data-store-db.user_name}"
    DATA_STORE_DB_PASSWORD = "${module.data-store-db.postgresql_password}"

    ENABLE_DB_MIGRATE = "false"

    DEFINITION_STORE_HOST               = "http://ccd-definition-store-api-${local.env_ase_url}"
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

    ELASTIC_SEARCH_HOSTS                = "${format("http://%s:9200", data.azurerm_key_vault_secret.ccd_elastic_search_url.value)}"
    ELASTIC_SEARCH_BLACKLIST            = "${var.elastic_search_blacklist}"
    ELASTIC_SEARCH_CASE_INDEX_NAME_FORMAT = "${var.elastic_search_case_index_name_format}"
    ELASTIC_SEARCH_CASE_INDEX_TYPE      = "${var.elastic_search_case_index_type}"
  }

}

module "data-store-db" {
  source = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product = "${local.app_full_name}-postgres-db"
  location = "${var.location}"
  env = "${var.env}"
  postgresql_user = "${var.postgresql_user}"
  database_name = "${var.database_name}"
  sku_name = "GP_Gen5_2"
  sku_tier = "GeneralPurpose"
  storage_mb = "51200"
  common_tags  = "${var.common_tags}"
}

////////////////////////////////
// Populate Vault with DB info
////////////////////////////////

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name = "${local.app_full_name}-POSTGRES-USER"
  value = "${module.data-store-db.user_name}"
  vault_uri = "${data.azurerm_key_vault.ccd_shared_key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name = "${local.app_full_name}-POSTGRES-PASS"
  value = "${module.data-store-db.postgresql_password}"
  vault_uri = "${data.azurerm_key_vault.ccd_shared_key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name = "${local.app_full_name}-POSTGRES-HOST"
  value = "${module.data-store-db.host_name}"
  vault_uri = "${data.azurerm_key_vault.ccd_shared_key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name = "${local.app_full_name}-POSTGRES-PORT"
  value = "${module.data-store-db.postgresql_listen_port}"
  vault_uri = "${data.azurerm_key_vault.ccd_shared_key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name = "${local.app_full_name}-POSTGRES-DATABASE"
  value = "${module.data-store-db.postgresql_database}"
  vault_uri = "${data.azurerm_key_vault.ccd_shared_key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "ccd_draft_encryption_key" {
  name = "${local.app_full_name}-draftStoreEncryptionSecret"
  value = "${random_string.draft_encryption_key.result}"
  vault_uri = "${data.azurerm_key_vault.ccd_shared_key_vault.vault_uri}"
}
