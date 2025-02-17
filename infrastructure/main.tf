provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
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
  force_user_permissions_trigger = "2"

  pgsql_databases = [
    {
      name = var.database_name
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "plpgsql,pg_stat_statements,pg_buffercache,hypopg"
    },
    {
      name  = "logfiles.download_enable"
      value = "ON"
    },
    {
      name  = "logfiles.retention_days"
      value = "7"
    },
    {
      name = "pg_qs.query_capture_mode"
      value = "ALL"
    },
    {
      name  = "log_lock_waits"
      value = "on"
    },
    {
      name  = "pgms_wait_sampling.query_capture_mode"
      value = "ALL"
    }

  ]
  pgsql_version    = "15"
  product          = var.product
  name             = "${local.app_full_name}-postgres-db-v15"
  pgsql_sku        = var.pgsql_sku
  pgsql_storage_mb = var.pgsql_storage_mb
  auto_grow_enabled = var.auto_grow_enabled
}

////////////////////////////////////
// Populate KeyVault with DB info //
////////////////////////////////////

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

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "${var.component}-POSTGRES-PORT"
  value        = "5432"
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = var.database_name
  key_vault_id = data.azurerm_key_vault.ccd_shared_key_vault.id
}
