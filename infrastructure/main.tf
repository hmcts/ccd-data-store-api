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
    DATA_STORE_DB_HOST                  = "${module.postgres-data-store.host_name}"
    DATA_STORE_DB_PORT                  = "${module.postgres-data-store.postgresql_listen_port}"
    DATA_STORE_DB_NAME                  = "${module.postgres-data-store.postgresql_database}"
    DATA_STORE_DB_USERNAME              = "${module.postgres-data-store.user_name}"
    DATA_STORE_DB_PASSWORD              = "${module.postgres-data-store.postgresql_password}"

    DEFINITION_STORE_HOST               = "http://ccd-definition-store-api-${local.env_ase_url}"
    USER_PROFILE_HOST                   = "http://ccd-user-profile-api-${local.env_ase_url}"

    CCD_DM_DOMAIN                       = "${var.document_management_valid_domain}"

    IDAM_USER_URL                       = "${var.idam_api_url}"
    IDAM_S2S_URL                        = "${var.s2s_url}"
    DATA_STORE_IDAM_KEY                 = "${data.vault_generic_secret.ccd_data_s2s_key.data["value"]}"

    DATA_STORE_S2S_AUTHORISED_SERVICES  = "${var.authorised-services}"

    CCD_DEFAULTPRINTURL                 = "${var.default_print_url}"
  }

}

module "postgres-data-store" {
  source              = "git@github.com:hmcts/moj-module-postgres?ref=master"
  product             = "${local.app_full_name}-data-store"
  location            = "West Europe"
  env                 = "${var.env}"
  postgresql_user     = "ccd"
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
