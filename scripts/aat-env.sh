#!/usr/bin/env bash
#
# Source this before running data-store smoke or functional tests against AAT dependencies.
#
# Local app run:
#   source ./scripts/aat-env.sh
#   ./gradlew runRemoteAAT
#   ./gradlew --no-daemon smoke
#
# Full functional run:
#   AAT_ENV_MODE=full source ./scripts/aat-env.sh
#   ./gradlew --no-daemon functional
#
# Deployed AAT target:
#   CCD_DATA_STORE_TARGET=remote source ./scripts/aat-env.sh
#   ./gradlew --no-daemon smoke
#
# Local definition-store override:
#   export AAT_DEFINITION_STORE_HOST=http://localhost:4451
#   source ./scripts/aat-env.sh
#
# Requires Azure CLI access to ccd-aat and s2s-aat Key Vaults.

is_true() {
  case "${1:-}" in
    true|TRUE|True|1|yes|YES|Yes|y|Y|on|ON|On)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

should_refresh_secrets() {
  is_true "${AAT_ENV_REFRESH_SECRETS:-false}"
}

env_var_has_value() {
  local variable_name="$1"

  eval '[ -n "${'"${variable_name}"':-}" ]'
}

all_env_vars_have_values() {
  local variable_name

  for variable_name in "$@"; do
    env_var_has_value "${variable_name}" || return 1
  done
}

aat_env_secret_profile() {
  local requested_profile="${1:-${AAT_ENV_MODE:-smoke}}"

  case "${requested_profile}" in
    ""|smoke|default)
      printf '%s' "smoke"
      ;;
    functional|full|all)
      printf '%s' "full"
      ;;
    *)
      echo "ERROR: Unknown AAT env mode '${requested_profile}'. Use 'smoke' or 'full'." >&2
      return 1
      ;;
  esac
}

print_aat_env_help() {
  cat <<'EOF'
Usage:
  source ./scripts/aat-env.sh
  AAT_ENV_MODE=full source ./scripts/aat-env.sh
  AAT_ENV_REFRESH_SECRETS=true source ./scripts/aat-env.sh
  source ./scripts/aat-env.sh help

Modes:
  smoke   Default. Fetches only the required smoke/base BEFTA secrets.
  full    Fetches smoke/base secrets plus optional functional BEFTA users.

Environment:
  AAT_ENV_MODE               smoke or full. Positional 'functional'/'full' is also accepted.
  AAT_ENV_REFRESH_SECRETS    true to discard already-exported AAT secret values before fetching.
  AAT_DEFINITION_STORE_HOST  optional local definition-store override, for example http://localhost:4451.
                              Leave unset to use DEFINITION_STORE_HOST from .aat-remote-env.
  CCD_DATA_STORE_TARGET      local by default, or remote/aat for the deployed AAT service URL.
  GROUP_ACCESS_ENABLED       defaults to true and is printed in the export summary.
  ENABLE_CASE_GROUP_ACCESS_FILTERING defaults to GROUP_ACCESS_ENABLED.

App runtime values still come from .aat-remote-env. Refresh that file with:
  ./gradlew reloadEnvSecrets -Penv=aat
EOF
}

require_az_cli() {
  if ! command -v az >/dev/null 2>&1; then
    echo "ERROR: Azure CLI 'az' was not found on PATH." >&2
    echo "Install Azure CLI, then run 'az login' before sourcing this script." >&2
    return 1
  fi
}

require_azure_keyvault_token() {
  local account_output
  local token_output

  if ! account_output="$(az account show --query '{name:name, tenantId:tenantId, user:user.name}' -o json 2>&1)"; then
    echo "ERROR: Azure CLI is not logged in or cannot load the current account." >&2
    echo "${account_output}" >&2
    echo "Run 'az login', then retry: source ./scripts/aat-env.sh" >&2
    return 1
  fi

  if ! token_output="$(az account get-access-token --resource https://vault.azure.net --query expiresOn -o tsv 2>&1)"; then
    echo "ERROR: Azure CLI cannot get a Key Vault access token." >&2
    echo "${token_output}" >&2
    echo "If the error mentions login.microsoftonline.com, fix Azure CLI internet/proxy/VPN access first." >&2
    echo "Useful check: az account get-access-token --resource https://vault.azure.net -o table" >&2
    return 1
  fi
}

kv_secret() {
  local vault_name="$1"
  local secret_name="$2"
  local secret_value

  if ! secret_value="$(az keyvault secret show \
    --vault-name "${vault_name}" \
    --name "${secret_name}" \
    --query value \
    -o tsv)"; then
    return 1
  fi

  secret_value="${secret_value//$'\r'/}"
  secret_value="${secret_value//$'\n'/}"
  printf '%s' "${secret_value}"
}

export_secret() {
  local variable_name="$1"
  local vault_name="$2"
  local secret_name="$3"
  local secret_value

  if ! should_refresh_secrets && env_var_has_value "${variable_name}"; then
    return 0
  fi

  if ! secret_value="$(kv_secret "${vault_name}" "${secret_name}")"; then
    echo "ERROR: Failed to read '${secret_name}' from Key Vault '${vault_name}'." >&2
    echo "Confirm your Azure account has access to the AAT Key Vaults, then retry." >&2
    return 1
  fi

  if [ -z "${secret_value}" ]; then
    echo "ERROR: Secret '${secret_name}' from Key Vault '${vault_name}' returned an empty value." >&2
    return 1
  fi

  export "${variable_name}=${secret_value}"
}

export_optional_secret() {
  local variable_name="$1"
  local vault_name="$2"
  local secret_name="$3"
  local secret_value

  if ! should_refresh_secrets && env_var_has_value "${variable_name}"; then
    return 0
  fi

  if secret_value="$(kv_secret "${vault_name}" "${secret_name}" 2>/dev/null)"; then
    export "${variable_name}=${secret_value}"
  else
    echo "WARN ${vault_name}/${secret_name}: not available; ${variable_name} not exported." >&2
  fi
}

export_secret_aliases() {
  local vault_name="$1"
  local secret_name="$2"
  shift 2
  local secret_value
  local variable_name

  if ! should_refresh_secrets && all_env_vars_have_values "$@"; then
    return 0
  fi

  if ! secret_value="$(kv_secret "${vault_name}" "${secret_name}")"; then
    echo "ERROR: Failed to read '${secret_name}' from Key Vault '${vault_name}'." >&2
    return 1
  fi

  if [ -z "${secret_value}" ]; then
    echo "ERROR: Secret '${secret_name}' from Key Vault '${vault_name}' returned an empty value." >&2
    return 1
  fi

  for variable_name in "$@"; do
    export "${variable_name}=${secret_value}"
  done
}

export_secret_list() {
  local secret_mode="$1"
  local line
  local variable_name
  local vault_name
  local secret_name

  while IFS='|' read -r variable_name vault_name secret_name; do
    case "${variable_name}" in
      ""|\#*)
        continue
        ;;
    esac

    case "${secret_mode}" in
      required)
        export_secret "${variable_name}" "${vault_name}" "${secret_name}" || return 1
        ;;
      optional)
        export_optional_secret "${variable_name}" "${vault_name}" "${secret_name}"
        ;;
      *)
        echo "ERROR: Unknown secret export mode '${secret_mode}'." >&2
        return 1
        ;;
    esac
  done
}

clear_aat_connection_values() {
  unset IDAM_API_URL_BASE IDAM_API_BASE_URL IDAM_URL IDAM_USER_URL IDAM_OIDC_URL OIDC_ISSUER
  unset S2S_URL_BASE S2S_URL IDAM_S2S_URL
  unset DEFINITION_STORE_HOST DEFINITION_STORE_URL_BASE
  unset CASE_DOCUMENT_AM_URL USER_PROFILE_HOST
  unset ROLE_ASSIGNMENT_URL ROLE_ASSIGNMENT_HOST
  unset RD_LOCATION_REF_API_BASE_URL RD_PROFESSIONAL_API_BASE_URL
  unset DM_STORE_BASE_URL BEFTA_TEST_STUB_SERVICE_BASE_URL
  unset OAUTH2_CLIENT_ID CCD_API_GATEWAY_OAUTH2_CLIENT_ID
  unset OAUTH2_REDIRECT_URI CCD_API_GATEWAY_OAUTH2_REDIRECT_URL
  unset TEST_URL CCD_DATA_STORE_API_BASE_URL
}

clear_aat_secret_values() {
  unset OAUTH2_CLIENT_SECRET CCD_API_GATEWAY_OAUTH2_CLIENT_SECRET
  unset CCD_API_GATEWAY_S2S_KEY BEFTA_S2S_CLIENT_SECRET CCD_GW_SERVICE_SECRET
  unset ROLE_ASSIGNMENT_API_GATEWAY_S2S_CLIENT_KEY BEFTA_S2S_CLIENT_SECRET_OF_CCD_DATA
  unset BEFTA_S2S_CLIENT_SECRET_OF_XUI_WEBAPP BEFTA_S2S_CLIENT_SECRET_OF_AAC_MANAGE_CASE_ASSIGNMENT
  unset DEFINITION_IMPORTER_USERNAME DEFINITION_IMPORTER_PASSWORD
  unset CCD_CASEWORKER_AUTOTEST_EMAIL CCD_CASEWORKER_AUTOTEST_PASSWORD
  unset CCD_IMPORT_AUTOTEST_EMAIL CCD_IMPORT_AUTOTEST_PASSWORD
  unset ROLE_ASSIGNMENT_USER_EMAIL ROLE_ASSIGNMENT_USER_PASSWORD
  unset CCD_PRIVATE_CASEWORKER_EMAIL CCD_PRIVATE_CASEWORKER_PASSWORD
  unset CCD_PRIVATE_CASEWORKER_AUTOTEST_1AND2_PASSWORD
  unset CCD_RESTRICTED_CASEWORKER_EMAIL CCD_RESTRICTED_CASEWORKER_PASSWORD
  unset CCD_PRIVATE_CASEWORKER_SOLICITOR_EMAIL CCD_PRIVATE_CASEWORKER_SOLICITOR_PASSWORD
  unset CCD_PRIVATE_CROSS_CASE_TYPE_CASEWORKER_EMAIL CCD_PRIVATE_CROSS_CASE_TYPE_CASEWORKER_PASSWORD
  unset CCD_PRIVATE_CROSS_CASE_TYPE_SOLICITOR_EMAIL CCD_PRIVATE_CROSS_CASE_TYPE_SOLICITOR_PASSWORD
  unset CCD_RESTRICTED_CROSS_CASE_TYPE_CASEWORKER_EMAIL CCD_RESTRICTED_CROSS_CASE_TYPE_CASEWORKER_PASSWORD
  unset CCD_BEFTA_CASEWORKER_2_SOLICITOR_1_PWD CCD_BEFTA_CASEWORKER_2_SOLICITOR_2_PWD
  unset CCD_BEFTA_CASEWORKER_2_SOLICITOR_3_PWD CCD_BEFTA_CASEWORKER_1_PWD
  unset CCD_BEFTA_CASEWORKER_2_PWD CCD_BEFTA_CASEWORKER_3_PWD
  unset CCD_BEFTA_CITIZEN_2_PWD CCD_BEFTA_CITIZEN_3_PWD
  unset CCD_BEFTA_SOLICITOR_3_PWD CCD_BEFTA_CASEWORKER_1_NO_PROFILE_PWD
  unset CCD_BEFTA_CASEWORKER_CAA_PWD CCD_BEFTA_MASTER_CASEWORKER_PWD
  unset CCD_BEFTA_MASTER_SOLICITOR_1_PWD CCD_BEFTA_MASTER_SOLICITOR_2_PWD
  unset CCD_BEFTA_MASTER_SOLICITOR_4_PWD
}

smoke_secret_values_loaded() {
  all_env_vars_have_values \
    OAUTH2_CLIENT_SECRET CCD_API_GATEWAY_OAUTH2_CLIENT_SECRET \
    CCD_API_GATEWAY_S2S_KEY BEFTA_S2S_CLIENT_SECRET CCD_GW_SERVICE_SECRET \
    ROLE_ASSIGNMENT_API_GATEWAY_S2S_CLIENT_KEY BEFTA_S2S_CLIENT_SECRET_OF_CCD_DATA \
    DEFINITION_IMPORTER_USERNAME DEFINITION_IMPORTER_PASSWORD \
    CCD_CASEWORKER_AUTOTEST_EMAIL CCD_CASEWORKER_AUTOTEST_PASSWORD \
    CCD_IMPORT_AUTOTEST_EMAIL CCD_IMPORT_AUTOTEST_PASSWORD \
    ROLE_ASSIGNMENT_USER_EMAIL ROLE_ASSIGNMENT_USER_PASSWORD
}

full_secret_values_loaded() {
  smoke_secret_values_loaded && all_env_vars_have_values \
    BEFTA_S2S_CLIENT_SECRET_OF_XUI_WEBAPP BEFTA_S2S_CLIENT_SECRET_OF_AAC_MANAGE_CASE_ASSIGNMENT \
    CCD_PRIVATE_CASEWORKER_EMAIL CCD_PRIVATE_CASEWORKER_PASSWORD \
    CCD_PRIVATE_CASEWORKER_AUTOTEST_1AND2_PASSWORD \
    CCD_RESTRICTED_CASEWORKER_EMAIL CCD_RESTRICTED_CASEWORKER_PASSWORD \
    CCD_PRIVATE_CASEWORKER_SOLICITOR_EMAIL CCD_PRIVATE_CASEWORKER_SOLICITOR_PASSWORD \
    CCD_PRIVATE_CROSS_CASE_TYPE_CASEWORKER_EMAIL CCD_PRIVATE_CROSS_CASE_TYPE_CASEWORKER_PASSWORD \
    CCD_PRIVATE_CROSS_CASE_TYPE_SOLICITOR_EMAIL CCD_PRIVATE_CROSS_CASE_TYPE_SOLICITOR_PASSWORD \
    CCD_RESTRICTED_CROSS_CASE_TYPE_CASEWORKER_EMAIL CCD_RESTRICTED_CROSS_CASE_TYPE_CASEWORKER_PASSWORD \
    CCD_BEFTA_CASEWORKER_2_SOLICITOR_1_PWD CCD_BEFTA_CASEWORKER_2_SOLICITOR_2_PWD \
    CCD_BEFTA_CASEWORKER_2_SOLICITOR_3_PWD CCD_BEFTA_CASEWORKER_1_PWD \
    CCD_BEFTA_CASEWORKER_2_PWD CCD_BEFTA_CASEWORKER_3_PWD \
    CCD_BEFTA_CITIZEN_2_PWD CCD_BEFTA_CITIZEN_3_PWD \
    CCD_BEFTA_SOLICITOR_3_PWD CCD_BEFTA_CASEWORKER_1_NO_PROFILE_PWD \
    CCD_BEFTA_CASEWORKER_CAA_PWD CCD_BEFTA_MASTER_CASEWORKER_PWD \
    CCD_BEFTA_MASTER_SOLICITOR_1_PWD CCD_BEFTA_MASTER_SOLICITOR_2_PWD \
    CCD_BEFTA_MASTER_SOLICITOR_4_PWD
}

needs_keyvault_access() {
  local secret_profile="$1"

  should_refresh_secrets && return 0

  case "${secret_profile}" in
    smoke)
      ! smoke_secret_values_loaded
      ;;
    full)
      ! full_secret_values_loaded
      ;;
    *)
      return 0
      ;;
  esac
}

export_remote_env_file() {
  local env_name="${1:-aat}"
  local env_file="${CCD_DATA_STORE_REMOTE_ENV_FILE:-.${env_name}-remote-env}"
  local line
  local key
  local value

  if [ ! -f "${env_file}" ]; then
    return 0
  fi

  while IFS= read -r line || [ -n "${line}" ]; do
    case "${line}" in
      ""|\#*|*=*)
        ;;
      *)
        continue
        ;;
    esac

    case "${line}" in
      ""|\#*)
        continue
        ;;
    esac

    key="${line%%=*}"
    value="${line#*=}"

    case "${key}" in
      [A-Za-z_]*)
        case "${key}" in
          *[!A-Za-z0-9_]*)
            continue
            ;;
        esac
        ;;
      *)
        continue
        ;;
    esac

    export "${key}=${value}"
  done < "${env_file}"

  export CCD_DATA_STORE_REMOTE_ENV_FILE_LOADED="${env_file}"
}

export_remote_defaults() {
  # IDAM: BEFTA /oauth2/authorize must use idam-api, not idam-web-public.
  export IDAM_API_URL_BASE="${IDAM_API_URL_BASE:-${IDAM_API_BASE_URL:-https://idam-api.aat.platform.hmcts.net}}"
  export IDAM_API_BASE_URL="${IDAM_API_BASE_URL:-${IDAM_API_URL_BASE}}"
  export IDAM_URL="${IDAM_URL:-${IDAM_API_URL_BASE}}"
  export IDAM_USER_URL="${IDAM_USER_URL:-https://idam-web-public.aat.platform.hmcts.net}"
  export IDAM_OIDC_URL="${IDAM_OIDC_URL:-https://idam-web-public.aat.platform.hmcts.net}"
  export OIDC_ISSUER="${OIDC_ISSUER:-${IDAM_OIDC_URL}/o}"

  export S2S_URL_BASE="${S2S_URL_BASE:-${IDAM_S2S_URL:-http://rpe-service-auth-provider-aat.service.core-compute-aat.internal}}"
  export S2S_URL="${S2S_URL:-${S2S_URL_BASE}}"
  export IDAM_S2S_URL="${IDAM_S2S_URL:-${S2S_URL_BASE}}"

  export DEFINITION_STORE_HOST="${DEFINITION_STORE_HOST:-http://ccd-definition-store-api-aat.service.core-compute-aat.internal}"
  export DEFINITION_STORE_URL_BASE="${DEFINITION_STORE_URL_BASE:-${DEFINITION_STORE_HOST}}"
  export CASE_DOCUMENT_AM_URL="${CASE_DOCUMENT_AM_URL:-http://ccd-case-document-am-api-aat.service.core-compute-aat.internal}"
  export USER_PROFILE_HOST="${USER_PROFILE_HOST:-http://ccd-user-profile-api-aat.service.core-compute-aat.internal}"
  export ROLE_ASSIGNMENT_URL="${ROLE_ASSIGNMENT_URL:-${ROLE_ASSIGNMENT_HOST:-http://am-role-assignment-service-aat.service.core-compute-aat.internal}}"
  export ROLE_ASSIGNMENT_HOST="${ROLE_ASSIGNMENT_HOST:-${ROLE_ASSIGNMENT_URL}}"
  export RD_LOCATION_REF_API_BASE_URL="${RD_LOCATION_REF_API_BASE_URL:-http://rd-location-ref-api-aat.service.core-compute-aat.internal}"
  export RD_PROFESSIONAL_API_BASE_URL="${RD_PROFESSIONAL_API_BASE_URL:-http://rd-professional-api-aat.service.core-compute-aat.internal}"
  export DM_STORE_BASE_URL="${DM_STORE_BASE_URL:-http://dm-store-aat.service.core-compute-aat.internal}"
  export BEFTA_TEST_STUB_SERVICE_BASE_URL="${BEFTA_TEST_STUB_SERVICE_BASE_URL:-http://ccd-test-stubs-service-aat.service.core-compute-aat.internal}"
}

apply_local_definition_store_override() {
  if [ -n "${AAT_DEFINITION_STORE_HOST:-}" ]; then
    export DEFINITION_STORE_HOST="${AAT_DEFINITION_STORE_HOST}"
    export DEFINITION_STORE_URL_BASE="${AAT_DEFINITION_STORE_HOST}"
  fi
}

export_oauth_values() {
  export OAUTH2_CLIENT_ID="${OAUTH2_CLIENT_ID:-ccd_gateway}"
  export CCD_API_GATEWAY_OAUTH2_CLIENT_ID="${CCD_API_GATEWAY_OAUTH2_CLIENT_ID:-${OAUTH2_CLIENT_ID}}"
  export OAUTH2_REDIRECT_URI="${OAUTH2_REDIRECT_URI:-https://www-ccd.nonprod.platform.hmcts.net/oauth2redirect}"
  export CCD_API_GATEWAY_OAUTH2_REDIRECT_URL="${CCD_API_GATEWAY_OAUTH2_REDIRECT_URL:-${OAUTH2_REDIRECT_URI}}"

  export_secret_aliases ccd-aat ccd-api-gateway-oauth2-client-secret \
    OAUTH2_CLIENT_SECRET \
    CCD_API_GATEWAY_OAUTH2_CLIENT_SECRET
}

export_s2s_values() {
  local secret_profile="$1"

  export CCD_API_GATEWAY_S2S_ID="${CCD_API_GATEWAY_S2S_ID:-ccd_gw}"
  export ROLE_ASSIGNMENT_API_GATEWAY_S2S_CLIENT_ID="${ROLE_ASSIGNMENT_API_GATEWAY_S2S_CLIENT_ID:-ccd_data}"
  export_secret_aliases s2s-aat microservicekey-ccd-gw \
    CCD_API_GATEWAY_S2S_KEY \
    BEFTA_S2S_CLIENT_SECRET \
    CCD_GW_SERVICE_SECRET \
    || return 1
  export_secret_aliases s2s-aat microservicekey-ccd-data \
    ROLE_ASSIGNMENT_API_GATEWAY_S2S_CLIENT_KEY \
    BEFTA_S2S_CLIENT_SECRET_OF_CCD_DATA \
    || return 1

  export CCD_GW_SERVICE_NAME="${CCD_GW_SERVICE_NAME:-${CCD_API_GATEWAY_S2S_ID}}"
  export BEFTA_S2S_CLIENT_ID="${BEFTA_S2S_CLIENT_ID:-${CCD_API_GATEWAY_S2S_ID}}"
  export BEFTA_S2S_CLIENT_ID_OF_CCD_DATA="${BEFTA_S2S_CLIENT_ID_OF_CCD_DATA:-ccd_data}"
  export BEFTA_S2S_CLIENT_ID_OF_XUI_WEBAPP="${BEFTA_S2S_CLIENT_ID_OF_XUI_WEBAPP:-xui_webapp}"

  if [ "${secret_profile}" = "full" ]; then
    export_optional_secret BEFTA_S2S_CLIENT_SECRET_OF_XUI_WEBAPP s2s-aat microservicekey-xui-webapp
    export_optional_secret BEFTA_S2S_CLIENT_SECRET_OF_AAC_MANAGE_CASE_ASSIGNMENT \
      s2s-aat microservicekey-aac-manage-case-assignment
  fi
}

export_required_user_secrets() {
  export_secret_list required <<'EOF'
DEFINITION_IMPORTER_USERNAME|ccd-aat|definition-importer-username
DEFINITION_IMPORTER_PASSWORD|ccd-aat|definition-importer-password
CCD_CASEWORKER_AUTOTEST_EMAIL|ccd-aat|ccd-caseworker-autotest-email
CCD_CASEWORKER_AUTOTEST_PASSWORD|ccd-aat|ccd-caseworker-autotest-password
CCD_IMPORT_AUTOTEST_EMAIL|ccd-aat|ccd-importer-autotest-email
CCD_IMPORT_AUTOTEST_PASSWORD|ccd-aat|ccd-importer-autotest-password
ROLE_ASSIGNMENT_USER_EMAIL|ccd-aat|idam-data-store-system-user-username
ROLE_ASSIGNMENT_USER_PASSWORD|ccd-aat|idam-data-store-system-user-password
EOF
}

export_full_functional_secrets() {
  export_secret_list optional <<'EOF'
CCD_PRIVATE_CASEWORKER_EMAIL|ccd-aat|ccd-private-caseworker-email
CCD_PRIVATE_CASEWORKER_PASSWORD|ccd-aat|ccd-private-caseworker-password
CCD_PRIVATE_CASEWORKER_AUTOTEST_1AND2_PASSWORD|ccd-aat|ccd-private-caseworker-autotest-1and2-password
CCD_RESTRICTED_CASEWORKER_EMAIL|ccd-aat|ccd-restricted-caseworker-email
CCD_RESTRICTED_CASEWORKER_PASSWORD|ccd-aat|ccd-restricted-caseworker-password
CCD_PRIVATE_CASEWORKER_SOLICITOR_EMAIL|ccd-aat|ccd-private-caseworker-solicitor-email
CCD_PRIVATE_CASEWORKER_SOLICITOR_PASSWORD|ccd-aat|ccd-private-caseworker-solicitor-password
CCD_PRIVATE_CROSS_CASE_TYPE_CASEWORKER_EMAIL|ccd-aat|ccd-private-cross-case-type-worker-email
CCD_PRIVATE_CROSS_CASE_TYPE_CASEWORKER_PASSWORD|ccd-aat|ccd-private-cross-case-type-caseworker-password
CCD_PRIVATE_CROSS_CASE_TYPE_SOLICITOR_EMAIL|ccd-aat|ccd-private-cross-case-type-solicitor-email
CCD_PRIVATE_CROSS_CASE_TYPE_SOLICITOR_PASSWORD|ccd-aat|ccd-private-cross-case-type-solicitor-password
CCD_RESTRICTED_CROSS_CASE_TYPE_CASEWORKER_EMAIL|ccd-aat|ccd-restricted-cross-case-type-caseworker-email
CCD_RESTRICTED_CROSS_CASE_TYPE_CASEWORKER_PASSWORD|ccd-aat|ccd-restricted-cross-case-type-caseworker-password
CCD_BEFTA_CASEWORKER_2_SOLICITOR_1_PWD|ccd-aat|ccd-befta-caseworker-2-solicitor-1-pwd
CCD_BEFTA_CASEWORKER_2_SOLICITOR_2_PWD|ccd-aat|ccd-befta-caseworker-2-solicitor-2-pwd
CCD_BEFTA_CASEWORKER_2_SOLICITOR_3_PWD|ccd-aat|ccd-befta-caseworker-2-solicitor-3-pwd
CCD_BEFTA_CASEWORKER_1_PWD|ccd-aat|ccd-befta-caseworker-1-pwd
CCD_BEFTA_CASEWORKER_2_PWD|ccd-aat|ccd-befta-caseworker-2-pwd
CCD_BEFTA_CASEWORKER_3_PWD|ccd-aat|ccd-befta-caseworker-3-pwd
CCD_BEFTA_CITIZEN_2_PWD|ccd-aat|ccd-befta-citizen-2-pwd
CCD_BEFTA_CITIZEN_3_PWD|ccd-aat|ccd-befta-citizen-3-pwd
CCD_BEFTA_SOLICITOR_3_PWD|ccd-aat|ccd-befta-solicitor-3-pwd
CCD_BEFTA_CASEWORKER_1_NO_PROFILE_PWD|ccd-aat|ccd-befta-caseworker-1-no-profile-pwd
CCD_BEFTA_CASEWORKER_CAA_PWD|ccd-aat|ccd-befta-caseworker-caa-pwd
CCD_BEFTA_MASTER_CASEWORKER_PWD|ccd-aat|ccd-befta-master-caseworker-pwd
CCD_BEFTA_MASTER_SOLICITOR_1_PWD|ccd-aat|ccd-befta-master-solicitor1-pwd
CCD_BEFTA_MASTER_SOLICITOR_2_PWD|ccd-aat|ccd-befta-master-solicitor2-pwd
CCD_BEFTA_MASTER_SOLICITOR_4_PWD|ccd-aat|ccd-befta-master-solicitor4-pwd
EOF
}

export_test_target() {
  case "${CCD_DATA_STORE_TARGET:-local}" in
    local)
      export TEST_URL="${TEST_URL:-http://localhost:4452}"
      ;;
    remote|staging|aat)
      export TEST_URL="${TEST_URL:-http://ccd-data-store-api-aat.service.core-compute-aat.internal}"
      ;;
    *)
      echo "Unknown CCD_DATA_STORE_TARGET='${CCD_DATA_STORE_TARGET}'. Use 'local' or 'remote'." >&2
      return 1
      ;;
  esac

  export CCD_DATA_STORE_API_BASE_URL="${TEST_URL}"
}

export_befta_knobs() {
  export BEFTA_RESPONSE_HEADER_CHECK_POLICY="${BEFTA_RESPONSE_HEADER_CHECK_POLICY:-JUST_WARN}"
  export BEFTA_RETRY_MAX_ATTEMPTS="${BEFTA_RETRY_MAX_ATTEMPTS:-3}"
  export BEFTA_RETRY_STATUS_CODES="${BEFTA_RETRY_STATUS_CODES:-500,502,503,504}"
  export BEFTA_RETRY_MAX_DELAY="${BEFTA_RETRY_MAX_DELAY:-1000}"
  export BEFTA_RETRY_NON_RETRYABLE_HTTP_METHODS="${BEFTA_RETRY_NON_RETRYABLE_HTTP_METHODS:-POST,PUT}"
  export DEFAULT_COLLECTION_ASSERTION_MODE="${DEFAULT_COLLECTION_ASSERTION_MODE:-UNORDERED}"
  export GROUP_ACCESS_ENABLED="${GROUP_ACCESS_ENABLED:-true}"
  export ENABLE_CASE_GROUP_ACCESS_FILTERING="${ENABLE_CASE_GROUP_ACCESS_FILTERING:-${GROUP_ACCESS_ENABLED}}"
}

print_aat_env_summary() {
  local secret_profile="$1"

  echo "AAT environment exported:"
  echo "  AAT_ENV_MODE=${secret_profile}"
  echo "  IDAM_API_URL_BASE=${IDAM_API_URL_BASE}"
  echo "  S2S_URL_BASE=${S2S_URL_BASE}"
  echo "  OAUTH2_CLIENT_ID=${OAUTH2_CLIENT_ID}"
  echo "  OAUTH2_REDIRECT_URI=${OAUTH2_REDIRECT_URI}"
  echo "  DEFINITION_STORE_URL_BASE=${DEFINITION_STORE_URL_BASE}"
  echo "  TEST_URL=${TEST_URL}"
  echo "  GROUP_ACCESS_ENABLED=${GROUP_ACCESS_ENABLED}"
  echo "  ENABLE_CASE_GROUP_ACCESS_FILTERING=${ENABLE_CASE_GROUP_ACCESS_FILTERING}"
}

aat_env_main() {
  local secret_profile

  case "${1:-}" in
    help|-h|--help)
      print_aat_env_help
      return 0
      ;;
  esac

  secret_profile="$(aat_env_secret_profile "$1")" || return 1

  if should_refresh_secrets; then
    clear_aat_secret_values
  fi

  if needs_keyvault_access "${secret_profile}"; then
    require_az_cli || return 1
    require_azure_keyvault_token || return 1
  fi

  clear_aat_connection_values
  export_remote_env_file aat || return 1
  export_remote_defaults
  apply_local_definition_store_override
  export_oauth_values || return 1
  export_s2s_values "${secret_profile}" || return 1
  export_required_user_secrets || return 1

  if [ "${secret_profile}" = "full" ]; then
    export_full_functional_secrets || return 1
  fi

  export_test_target || return 1
  export_befta_knobs
  print_aat_env_summary "${secret_profile}"
}

aat_env_main "$@" || return 1 2>/dev/null || exit 1
