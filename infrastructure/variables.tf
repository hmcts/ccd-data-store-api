variable "product" {
  type    = "string"
  default = "ccd-data-store-api"
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

variable "database-name" {
  type    = "string"
  default = "ccd_data"
}

variable "vault_section" {
  default = "preprod"
}

variable "idam_api_url" {
  default = "https://preprod-idamapi.reform.hmcts.net:3511"
}

variable "s2s_url" {
  default = "https://preprod-s2s-api.reform.hmcts.net:3511"
}

variable "authorised-services" {
  type    = "string"
  default = "ccd_data,ccd_gw,ccd_ps,probate_backend,divorce_ccd_submission,sscs,cmc,cmc_claim_store"
}

variable "document_management_valid_domain" {
  default = "https://api-gateway.preprod.dm.reform.hmcts.net"
}

variable "default_print_url" {
  default = "https://return-case-doc-ccd.nonprod.platform.hmcts.net/jurisdictions/:jid/case-types/:ctid/cases/:cid"
}
