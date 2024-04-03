database_sku_name     = "GP_Gen5_32"
database_sku_capacity = "32"
# Note: AutoGrow is enabled in prod, and not supported by cnp-module-postgres module, so database_storage_mb > than current General Purpose Storage for TF.
database_storage_mb   = "2048000"

pgsql_sku        = "MO_Standard_E16s_v3"
pgsql_storage_mb = 4193280
auto_grow_enabled = true
subnet_suffix = "expanded"

enable_schema_ownership = true
kv_subscription = "DCD-CNP-Prod"
