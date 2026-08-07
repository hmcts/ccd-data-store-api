#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
env_file="${script_dir}/../.env"

if [ -e "$env_file" ]; then
  echo "Refusing to overwrite existing $env_file" >&2
  exit 1
fi

command -v openssl >/dev/null 2>&1 || {
  echo "openssl is required to generate local values" >&2
  exit 1
}

umask 077
db_password=$(openssl rand -hex 16)
token_secret=$(openssl rand -hex 32)

cat > "$env_file" <<EOF
# Generated for local Data Store Docker use. Do not commit or use for AAT/production.
DATA_STORE_DB_USERNAME=data_store
DATA_STORE_DB_PASSWORD=${db_password}
DATA_STORE_TOKEN_SECRET=${token_secret}
SERVER_PORT=4452
AZURE_APPLICATIONINSIGHTS_INSTRUMENTATIONKEY=local-only
EOF

chmod 600 "$env_file"
echo "Created $env_file with disposable local values."
