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

variable "database-name" {
  type    = "string"
  default = "ccd_data"
}

variable "authorised-services" {
  type    = "string"
  default = "ccd_data,ccd_gw,ccd_ps,probate_backend,divorce_ccd_submission,sscs,cmc,cmc_claim_store"
}

variable "subscription" {
  type    = "string"
}

variable "test-idam-api-url" {
  default = "http://betaDevBccidamAppLB.reform.hmcts.net"
}

variable "prod-idam-api-url" {
  default = "http://betaProdccidamAppLB.reform.hmcts.net:4501"
}

variable "test-s2s-url" {
  default = "http://betaDevBccidamS2SLB.reform.hmcts.net"
}

variable "prod-s2s-url" {
  default = "http://betaProdccidamAppLB.reform.hmcts.net:4502"
}

variable "ilbIp"{}
