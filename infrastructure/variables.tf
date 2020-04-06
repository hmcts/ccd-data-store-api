variable "product" {
  type = "string"
}

variable "raw_product" {
  default = "ccd" // jenkins-library overrides product for PRs and adds e.g. pr-118-ccd
}

variable "component" {
  type = "string"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "subscription" {
  type    = "string"
}

variable "common_tags" {
  type = "map"
}

variable "tenant_id" {
  description = "(Required) The Azure Active Directory tenant ID that should be used for authenticating requests to the key vault. This is usually sourced from environemnt variables and not normally required to be specified."
}

////////////////////////////////
// Database
////////////////////////////////

variable "postgresql_user" {
  default = "ccd"
}

variable "database_name" {
  default = "ccd_data_store"
}

variable "data_store_max_pool_size" {
  default = "16"
}

variable "database_sku_name" {
  default = "GP_Gen5_2"
}

variable "database_sku_capacity" {
  default = "2"
}

variable "database_storage_mb" {
  default = "51200"
}

