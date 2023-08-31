data "azurerm_application_insights" "ai" {
  name                = "${var.product}-${var.env}"
  resource_group_name = "${var.product}-shared-${var.env}"
}

data "azurerm_key_vault" "key_vault" {
  name                = "${var.product}-${var.env}"
  resource_group_name = "${var.product}-shared-${var.env}"
}
