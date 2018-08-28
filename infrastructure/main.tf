provider "vault" {
  //  # It is strongly recommended to configure this provider through the
  //  # environment variables described above, so that each user can have
  //  # separate credentials set in the environment.
  //  #
  //  # This will default to using $VAULT_ADDR
  //  # But can be set explicitly
  address = "https://vault.reform.hmcts.net:6200"
}

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
  previewVaultName = "${var.raw_product}-shared-aat"
  nonPreviewVaultName = "${var.raw_product}-shared-${var.env}"
  vaultName = "${(var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName}"

  // Old vault info to be removed
  oldPreviewVaultName = "${var.product}-data-store"
  oldNonPreviewVaultName = "ccd-data-store-${var.env}"
  oldVaultName = "${(var.env == "preview" || var.env == "spreview") ? local.oldPreviewVaultName : local.oldNonPreviewVaultName}"


  // S2S
  s2s_url = "http://rpe-service-auth-provider-${local.env_ase_url}"

  custom_redirect_uri = "${var.frontend_url}/oauth2redirect"
  default_redirect_uri = "https://ccd-case-management-web-${local.env_ase_url}/oauth2redirect"
  oauth2_redirect_uri = "${var.frontend_url != "" ? local.custom_redirect_uri : local.default_redirect_uri}"

  draftStoreUrl = "http://draft-store-service-${local.local_env}.service.${local.local_ase}.internal"
}

data "azurerm_key_vault" "ccd_shared_key_vault" {
  name = "${local.vaultName}"
  resource_group_name = "${local.vaultName}"
}

data "vault_generic_secret" "ccd_data_s2s_key" {
  path = "secret/${var.vault_section}/ccidam/service-auth-provider/api/microservice-keys/ccd-data"
}

data "vault_generic_secret" "gateway_idam_key" {
  path = "secret/${var.vault_section}/ccidam/service-auth-provider/api/microservice-keys/ccd-gw"
}

data "vault_generic_secret" "gateway_oauth2_client_secret" {
  path = "secret/${var.vault_section}/ccidam/idam-api/oauth2/client-secrets/ccd-gateway"
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
  source   = "git@github.com:hmcts/moj-module-webapp?ref=master"
  product  = "${local.app_full_name}"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"
  subscription = "${var.subscription}"
  is_frontend = false
  common_tags  = "${var.common_tags}"
  additional_host_name = "debugparam"
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
    DATA_STORE_IDAM_KEY                 = "${data.vault_generic_secret.ccd_data_s2s_key.data["value"]}"

    CCD_DRAFT_STORE_URL                 = "${local.draftStoreUrl}"
    CCD_DRAFT_TTL_DAYS                  = "${var.draft_store_ttl_days}"
    CCD_DRAFT_ENCRYPTION_KEY            = "${random_string.draft_encryption_key.result}"

    DATA_STORE_S2S_AUTHORISED_SERVICES  = "${var.authorised-services}"

    CCD_DEFAULTPRINTURL                 = "${local.default_print_url}"
  }

}

module "data-store-db" {
  source = "git@github.com:hmcts/moj-module-postgres?ref=master"
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

module "ccd-data-store-vault" {
  source              = "git@github.com:hmcts/moj-module-key-vault?ref=master"
  name                = "${local.oldVaultName}" // Max 24 characters
  product             = "${var.product}"
  env                 = "${var.env}"
  tenant_id           = "${var.tenant_id}"
  object_id           = "${var.jenkins_AAD_objectId}"
  resource_group_name = "${module.ccd-data-store-api.resource_group_name}"
  product_group_object_id = "be8b3850-998a-4a66-8578-da268b8abd6b"
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

resource "azurerm_key_vault_secret" "gw_s2s_key" {
  name = "ccd-api-gateway-idam-service-key"
  value = "${data.vault_generic_secret.gateway_idam_key.data["value"]}"
  vault_uri = "${data.azurerm_key_vault.ccd_shared_key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "gw_oauth2_secret" {
  name = "ccd-api-gateway-oauth2-client-secret"
  value = "${data.vault_generic_secret.gateway_oauth2_client_secret.data["value"]}"
  vault_uri = "${data.azurerm_key_vault.ccd_shared_key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "ccd_draft_encryption_key" {
  name = "draftStoreEncryptionSecret"
  value = "${random_string.draft_encryption_key.result}"
  vault_uri = "${module.ccd-data-store-vault.key_vault_uri}"
}
