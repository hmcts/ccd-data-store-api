database_sku_name     = "GP_Gen5_32"
database_sku_capacity = "32"
# Note: AutoGrow is enabled in prod, and not supported by cnp-module-postgres module, so database_storage_mb > than current General Purpose Storage for TF.
database_storage_mb   = "2048000"

# PG Flexible Server SKU
pgsql_sku = "GP_Standard_D32s_v3"
pgsql_storage_mb = "2048000"
