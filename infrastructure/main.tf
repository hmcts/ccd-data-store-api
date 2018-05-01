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
  env_ase_url = "${var.env}.service.${data.terraform_remote_state.core_apps_compute.ase_name[0]}.internal"
  default_default_print_url = "https://ccd-case-print-service-${local.env_ase_url}/jurisdictions/:jid/case-types/:ctid/cases/:cid"
  default_print_url = "${var.default_print_url != "" ? var.default_print_url : local.default_default_print_url}"

  default_dm_valid_domain = "^https?://(?:api-gateway\\.test\\.dm\\.reform\\.hmcts\\.net|dm-store-${var.env}\\.service\\.core-compute-${var.env}\\.internal(?::\\d+)?)"
  dm_valid_domain = "${var.document_management_valid_domain != "" ? var.document_management_valid_domain : local.default_dm_valid_domain}"
}

data "vault_generic_secret" "ccd_data_s2s_key" {
  path = "secret/${var.vault_section}/ccidam/service-auth-provider/api/microservice-keys/ccd-data"
}

module "ccd-data-store-api" {
  source   = "git@github.com:hmcts/moj-module-webapp?ref=master"
  product  = "${local.app_full_name}"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"
  subscription = "${var.subscription}"

  app_settings = {
    DATA_STORE_DB_HOST = "${var.use_uk_db != "true" ? module.postgres-data-store.host_name : module.data-store-db.host_name}"
    DATA_STORE_DB_PORT = "${var.use_uk_db != "true" ? module.postgres-data-store.postgresql_listen_port : module.data-store-db.postgresql_listen_port}"
    DATA_STORE_DB_NAME = "${var.use_uk_db != "true" ? module.postgres-data-store.postgresql_database : module.data-store-db.postgresql_database}"
    DATA_STORE_DB_USERNAME = "${var.use_uk_db != "true" ? module.postgres-data-store.user_name : module.data-store-db.user_name}"
    DATA_STORE_DB_PASSWORD = "${var.use_uk_db != "true" ? module.postgres-data-store.postgresql_password : module.data-store-db.postgresql_password}"

    DEFINITION_STORE_HOST               = "http://ccd-definition-store-api-${local.env_ase_url}"
    USER_PROFILE_HOST                   = "http://ccd-user-profile-api-${local.env_ase_url}"

    CCD_DM_DOMAIN                       = "${local.dm_valid_domain}"

    IDAM_USER_URL                       = "${var.idam_api_url}"
    IDAM_S2S_URL                        = "${var.s2s_url}"
    DATA_STORE_IDAM_KEY                 = "${data.vault_generic_secret.ccd_data_s2s_key.data["value"]}"

    DATA_STORE_S2S_AUTHORISED_SERVICES  = "${var.authorised-services}"

    CCD_DEFAULTPRINTURL                 = "${local.default_print_url}"
  }

}

module "postgres-data-store" {
  source              = "git@github.com:hmcts/moj-module-postgres?ref=master"
  product             = "${local.app_full_name}-data-store"
  location            = "West Europe"
  env                 = "${var.env}"
  postgresql_user     = "ccd"
}

module "data-store-db" {
  source = "git@github.com:hmcts/moj-module-postgres?ref=cnp-449-tactical"
  product = "${local.app_full_name}-postgres-db"
  location = "${var.location}"
  env = "${var.env}"
  postgresql_user = "${var.postgresql_user}"
  database_name = "${var.database_name}"
  sku_name = "GP_Gen5_2"
  sku_tier = "GeneralPurpose"
  storage_mb = "51200"
}

module "ccd-data-store-vault" {
  source              = "git@github.com:hmcts/moj-module-key-vault?ref=master"
  name                = "ccd-data-store-${var.env}" // Max 24 characters
  product             = "${var.product}"
  env                 = "${var.env}"
  tenant_id           = "${var.tenant_id}"
  object_id           = "${var.jenkins_AAD_objectId}"
  resource_group_name = "${module.ccd-data-store-api.resource_group_name}"
  product_group_object_id = "be8b3850-998a-4a66-8578-da268b8abd6b"
}
