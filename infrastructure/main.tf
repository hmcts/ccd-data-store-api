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
  vault_section = "${var.env == "prod" ? "prod" : "test"}"

  idam_api_url = "${var.env == "prod" ? var.prod-idam-api-url : var.test-idam-api-url}"
  s2s_url = "${var.env == "prod" ? var.prod-s2s-url : var.test-s2s-url}"

  env_ase_url = "${var.env}.service.${data.terraform_remote_state.core_apps_compute.ase_name[0]}.internal"
}

data "vault_generic_secret" "ccd_data_s2s_key" {
  path = "secret/${local.vault_section}/ccidam/service-auth-provider/api/microservice-keys/ccd-data"
}

module "ccd-data-store-api" {
  source   = "git@github.com:contino/moj-module-webapp?ref=master"
  product  = "${var.product}"
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

    IDAM_USER_URL                       = "${local.idam_api_url}"
    IDAM_S2S_URL                        = "${local.s2s_url}"
    DATA_STORE_IDAM_KEY                 = "${data.vault_generic_secret.ccd_data_s2s_key.data["value"]}"

    CASEDATASTORE_AUTHORISED_SERVICES   = "${var.authorised-services}"
  }

}

module "postgres-data-store" {
  source              = "git@github.com:contino/moj-module-postgres?ref=master"
  product             = "${var.product}-data-store"
  location            = "West Europe"
  env                 = "${var.env}"
  postgresql_user     = "ccd"
}
