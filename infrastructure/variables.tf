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

variable "definition_cache_max_idle_sec" {
  type = "string"
  default = "259200"
}

variable "definition_cache_latest_version_ttl_sec" {
  type = "string"
  default = "1"
}

variable "definition_cache_max_size" {
  type = "string"
  default = "5000"
}

variable "definition_cache_eviction_policy" {
  type = "string"
  default = "NONE"
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

variable "authorised-services" {
  type    = "string"
  default = "ccd_data,ccd_gw,ccd_ps,probate_backend,divorce_ccd_submission,sscs,sscs_bulkscan,cmc,cmc_claim_store,cmc_claim_external_api,jui_webapp,pui_webapp,bulk_scan_orchestrator,fpl_case_service,iac,finrem_ccd_data_migrator,finrem_case_orchestration,employment_tribunals,ethos_repl_service,ccpay_bubble,ctsc_work_allocation,em_ccd_orchestrator,xui_webapp,bulk_scan_payment_processor"
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

variable "elastic_search_enabled" {
  default = "false"
}

variable "elastic_search_request_timeout" {
  default = "10000"
}

variable "elastic_search_case_index_name_format" {
  description = "Format of the elastic search index name for cases"
  type = "string"
  default = "%s_cases"
}

variable "elastic_search_case_index_type" {
  description = "Cases index document type"
  type = "string"
  default = "_doc"
}

variable "elastic_search_nodes_discovery_enabled" {
  description = "Enable Elasticsearch node discovery by Jest client"
  type = "string"
  default = "true"
}

variable "elastic_search_nodes_discovery_frequency_millis" {
  description = "Elasticsearch node discovery frequency in milliseconds"
  type = "string"
  default = "5000"
}

variable "elastic_search_nodes_discovery_filter" {
  description = "Elasticsearch node discovery filter"
  type = "string"
  default = "_all"
}

variable "http_client_connection_timeout" {
  type = "string"
  default = "30000"
}

variable "http_client_read_timeout" {
  type = "string"
  default = "60000"
}

variable "http_client_max_total" {
  type = "string"
  default = "100"
}

variable "http_client_seconds_idle_connection" {
  type = "string"
  default = "120"
}

variable "http_client_max_client_per_route" {
  type = "string"
  default = "20"
}

variable "http_client_validate_after_inactivity" {
  type = "string"
  default = "0"
}

variable "ccd_am_write_to_ccd_only" {
  type = "string"
  default = ""
}

variable "ccd_am_write_to_am_only" {
  type = "string"
  default = ""
}

variable "ccd_am_write_to_both" {
  type = "string"
  default = ""
}

variable "ccd_am_read_from_ccd" {
  type = "string"
  default = ""
}

variable "ccd_am_read_from_am" {
  type = "string"
  default = ""
}

variable "appinsights_instrumentation_key" {
  description = "Instrumentation key of the App Insights instance this webapp should use. Module will create own App Insights resource if this is not provided"
  default = ""
}

variable "additional_host_name" {
  description = "A custom domain name for this webapp."
  default = "null"
}

variable "enable_ase" {
  default = false
}

