#!/usr/bin/env bash

set -e

if [ -z "$DATA_STORE_DB_USERNAME" ] || [ -z "$DATA_STORE_DB_PASSWORD" ]; then
  echo "ERROR: Missing environment variable. Set value for both 'DATA_STORE_DB_USERNAME' and 'DATA_STORE_DB_PASSWORD'."
  exit 1
fi

# Create role and database
psql -v ON_ERROR_STOP=1 --username postgres --set USERNAME=$DATA_STORE_DB_USERNAME --set PASSWORD=$DATA_STORE_DB_PASSWORD <<-EOSQL
  CREATE USER :USERNAME WITH PASSWORD ':PASSWORD';
  CREATE DATABASE ccd_data
    WITH OWNER = :USERNAME
    ENCODING = 'UTF-8'
    CONNECTION LIMIT = -1;
  ALTER SCHEMA public OWNER TO :USERNAME;
EOSQL
