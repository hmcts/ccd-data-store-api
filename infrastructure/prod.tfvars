database_sku_name     = "GP_Gen5_32"
database_sku_capacity = "32"
# Note: AutoGrow is enabled in prod, and not supported by cnp-module-postgres module, so database_storage_mb > than current General Purpose Storage for TF.
database_storage_mb   = "2000000"
