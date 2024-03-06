provider "azurerm" {
  features {}
}

locals {
  app_full_name = "${var.product}-${var.component}"

  // Vault name
  vaultName = "${var.raw_product}-${var.env}"

  // Shared Resource Group
  sharedResourceGroup = "${var.raw_product}-shared-${var.env}"

  sharedASPResourceGroup = "${var.raw_product}-shared-${var.env}"

}

data "azurerm_key_vault" "ccd_shared_key_vault" {
  name                = "${local.vaultName}"
  resource_group_name = "${local.sharedResourceGroup}"
}

data "azurerm_key_vault" "s2s_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

data "azurerm_key_vault_secret" "ccd_data_s2s_key" {
  name         = "microservicekey-ccd-data"
  key_vault_id = "${data.azurerm_key_vault.s2s_vault.id}"
}

resource "azurerm_key_vault_secret" "ccd_data_s2s_secret" {
  name         = "ccd-data-s2s-secret"
  value        = data.azurerm_key_vault_secret.ccd_data_s2s_key.value
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}

resource "random_string" "draft_encryption_key" {
  length  = 16
  special = true
  upper   = true
  lower   = true
  number  = true
  lifecycle {
    ignore_changes = all
  }
}


////////////////////////////////
// Populate Vault with DB info
////////////////////////////////

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.component}-POSTGRES-USER"
  value        = module.data-store-db-v11.user_name
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.data-store-db-v11.postgresql_password
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "${var.component}-POSTGRES-HOST"
  value        = module.data-store-db-v11.host_name
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "${var.component}-POSTGRES-PORT"
  value        = module.data-store-db-v11.postgresql_listen_port
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = module.data-store-db-v11.postgresql_database
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}

resource "azurerm_key_vault_secret" "ccd_draft_encryption_key" {
  name         = "${var.component}-draftStoreEncryptionSecret"
  value        = random_string.draft_encryption_key.result
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}

resource "azurerm_key_vault_secret" "draft-store-key" {
  name         = "${var.component}-draft-key"
  value        = random_string.draft_encryption_key.result
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}

module "data-store-db-v11" {
  source          = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product         = var.product
  component       = var.component
  name            = "${local.app_full_name}-postgres-db-v11"
  location        = "${var.location}"
  env             = "${var.env}"
  subscription    = "${var.subscription}"
  postgresql_user = "${var.postgresql_user}"
  database_name   = "${var.database_name}"
  postgresql_version = "11"
  sku_name        = "${var.database_sku_name}"
  sku_tier        = "GeneralPurpose"
  sku_capacity    = "${var.database_sku_capacity}"
  storage_mb      = "${var.database_storage_mb}"
  common_tags     = "${var.common_tags}"
}

////////////////////////////////
// DB version 15              //
////////////////////////////////


module "postgresql_v15" {
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }
  
  admin_user_object_id = var.jenkins_AAD_objectId
  business_area        = "cft"
  common_tags          = var.common_tags
  component            = var.component
  env                  = var.env
  subnet_suffix        = var.subnet_suffix
  # Setup Access Reader db user
  force_user_permissions_trigger = "1"

  # Sets correct DB owner after migration to fix permissions
  enable_schema_ownership = var.enable_schema_ownership
  force_schema_ownership_trigger = "1"
  kv_subscription = var.kv_subscription
  kv_name = data.azurerm_key_vault.ccd_shared_key_vault.name
  user_secret_name = azurerm_key_vault_secret.POSTGRES-USER.name
  pass_secret_name = azurerm_key_vault_secret.POSTGRES-PASS.name

  pgsql_databases = [
    {
      name = var.database_name
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "plpgsql,pg_stat_statements,pg_buffercache,hypopg"
    }
  ]
  pgsql_version    = "15"
  product          = var.product
  name             = "${local.app_full_name}-postgres-db-v15"
  pgsql_sku        = var.pgsql_sku
  pgsql_storage_mb = var.pgsql_storage_mb
  auto_grow_enabled = var.auto_grow_enabled
}

resource "azurerm_key_vault_secret" "POSTGRES-USER-V15" {
  name         = "${var.component}-POSTGRES-USER-V15"
  value        = module.postgresql_v15.username
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS-V15" {
  name         = "${var.component}-POSTGRES-PASS-V15"
  value        = module.postgresql_v15.password
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-HOST-V15" {
  name         = "${var.component}-POSTGRES-HOST-V15"
  value        = module.postgresql_v15.fqdn
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}
