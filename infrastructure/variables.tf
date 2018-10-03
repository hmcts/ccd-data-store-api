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

variable "ilbIp"{}

variable "subscription" {
  type    = "string"
}

variable "capacity" {
  default = "1"
}

variable "common_tags" {
  type = "map"
}

variable "asp_name" {
  type = "string"
  description = "App Service Plan (ASP) to use for the webapp, 'use_shared' to make use of the shared ASP"
  default = "use_shared"
}

variable "asp_rg" {
  type = "string"
  description = "App Service Plan (ASP) resource group for 'asp_name', 'use_shared' to make use of the shared resource group"
  default = "use_shared"
}

variable "tenant_id" {
  description = "(Required) The Azure Active Directory tenant ID that should be used for authenticating requests to the key vault. This is usually sourced from environemnt variables and not normally required to be specified."
}

variable "client_id" {
  description = "(Required) The object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies. This is usually sourced from environment variables and not normally required to be specified."
}

variable "jenkins_AAD_objectId" {
  type                        = "string"
  description                 = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
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

variable "authorised-services" {
  type    = "string"
  default = "ccd_data,ccd_gw,ccd_ps,probate_backend,divorce_ccd_submission,sscs,cmc,cmc_claim_store,jui_webapp,pui_webapp"
}

variable "idam_api_url" {
  default = "http://betaDevBccidamAppLB.reform.hmcts.net"
}

variable "draft_store_ttl_days" {
  type = "string"
  default = "180"
}

variable "document_management_valid_domain" {
  type = "string"
  default = ""
}

variable "default_print_url" {
  type = "string"
  default = ""
}

variable "frontend_url" {
  description = "Optional front end URL to use for building redirect URI"
  type = "string"
  default = ""
}

variable "elastic_search_blacklist" {
  description = "Forbidden elastic search query types"
  type = "string"
  default = "query_string"
}

variable "elasticsearch_hosts" {
  type = "string"
  default = ""
}

variable "elastic_search_case_index_name_format" {
  description = "Format of the elastic search index name for cases"
  type = "string"
  default = "%s_cases"
}

variable "elastic_search_case_index_type" {
  description = "Cases index document type"
  type = "string"
  default = "case"
}
