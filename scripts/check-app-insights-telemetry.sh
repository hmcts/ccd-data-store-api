#!/usr/bin/env sh

set -eu

DEFAULT_APP_INSIGHTS_ENV="aat"
DEFAULT_APP_INSIGHTS_ROLE_NAME="ccd-data-store-api"
DEFAULT_APP_INSIGHTS_LOOKBACK="2h"
DEFAULT_APP_INSIGHTS_TIMEOUT_SECONDS=600
DEFAULT_APP_INSIGHTS_POLL_SECONDS=30
DEFAULT_APP_INSIGHTS_API_VERSION="2018-04-20"
PREVIEW_ENV="preview"
STATUS_PASS="PASS"
STATUS_FAIL="FAIL"
STATUS_SKIP="SKIP"
MIN_REQUIRED_TELEMETRY_COUNT=1
DISABLED_TELEMETRY_COUNT_EXPRESSION="0"
TELEMETRY_COUNT_COLUMNS=6
CONFIG_ERROR_EXIT_CODE=2
TELEMETRY_FAILURE_EXIT_CODE=1
ALLOW_UNSCOPED_TELEMETRY_CHECK="${ALLOW_UNSCOPED_TELEMETRY_CHECK:-false}"

APP_INSIGHTS_ENV="${APP_INSIGHTS_ENV:-$DEFAULT_APP_INSIGHTS_ENV}"
APP_INSIGHTS_APP_NAME="${APP_INSIGHTS_APP_NAME:-${APP_INSIGHTS_APP:-ccd-${APP_INSIGHTS_ENV}}}"
APP_INSIGHTS_RESOURCE_GROUP="${APP_INSIGHTS_RESOURCE_GROUP:-ccd-shared-${APP_INSIGHTS_ENV}}"
APP_INSIGHTS_ROLE_NAME="${APP_INSIGHTS_ROLE_NAME:-$DEFAULT_APP_INSIGHTS_ROLE_NAME}"
APP_INSIGHTS_LOOKBACK="${APP_INSIGHTS_LOOKBACK:-$DEFAULT_APP_INSIGHTS_LOOKBACK}"
APP_INSIGHTS_TIMEOUT_SECONDS="${APP_INSIGHTS_TIMEOUT_SECONDS:-$DEFAULT_APP_INSIGHTS_TIMEOUT_SECONDS}"
APP_INSIGHTS_POLL_SECONDS="${APP_INSIGHTS_POLL_SECONDS:-$DEFAULT_APP_INSIGHTS_POLL_SECONDS}"
APP_INSIGHTS_SOURCE_ENV="${APP_INSIGHTS_SOURCE_ENV:-${APP_INSIGHTS_ENV}}"
# Optional filters. Use at least one to scope shared App Insights data to this pipeline's app instance.
APP_INSIGHTS_REQUEST_URL_CONTAINS="${APP_INSIGHTS_REQUEST_URL_CONTAINS:-}"
APP_INSIGHTS_ROLE_INSTANCE_CONTAINS="${APP_INSIGHTS_ROLE_INSTANCE_CONTAINS:-}"
APP_INSIGHTS_TRACE_MARKER="${APP_INSIGHTS_TRACE_MARKER:-}"
APP_INSIGHTS_API_VERSION="${APP_INSIGHTS_API_VERSION:-$DEFAULT_APP_INSIGHTS_API_VERSION}"
REQUIRE_DEPENDENCY_TELEMETRY="${REQUIRE_DEPENDENCY_TELEMETRY:-true}"
REQUIRE_TRACE_TELEMETRY="${REQUIRE_TRACE_TELEMETRY:-true}"
attempt=1

if [ "$APP_INSIGHTS_SOURCE_ENV" = "$PREVIEW_ENV" ] && [ -z "$APP_INSIGHTS_REQUEST_URL_CONTAINS" ]; then
  case "${BRANCH_NAME:-}" in
    PR-*|pr-*)
      APP_INSIGHTS_REQUEST_URL_CONTAINS="$(printf '%s' "${APP_INSIGHTS_ROLE_NAME}-${BRANCH_NAME}.preview.platform.hmcts.net" | tr '[:upper:]' '[:lower:]')"
      ;;
  esac
fi

if [ "$APP_INSIGHTS_SOURCE_ENV" = "$PREVIEW_ENV" ] && [ -z "$APP_INSIGHTS_ROLE_INSTANCE_CONTAINS" ]; then
  case "${BRANCH_NAME:-}" in
    PR-*|pr-*)
      APP_INSIGHTS_ROLE_INSTANCE_CONTAINS="$(printf '%s' "${APP_INSIGHTS_ROLE_NAME}-${BRANCH_NAME}" | tr '[:upper:]' '[:lower:]')"
      ;;
  esac
fi

is_true() {
  case "$1" in
    true|TRUE|True|1|yes|YES|Yes) return 0 ;;
    *) return 1 ;;
  esac
}

is_positive_integer() {
  case "$1" in
    ''|*[!0-9]*) return 1 ;;
  esac

  [ "$1" -gt 0 ] 2>/dev/null
}

validate_positive_integer_config() {
  variable_name="$1"
  value="$2"

  if ! is_positive_integer "$value"; then
    echo "${variable_name} must be a positive integer, got '${value}'." >&2
    exit "$CONFIG_ERROR_EXIT_CODE"
  fi
}

validate_duration_config() {
  variable_name="$1"
  value="$2"
  duration_amount="${value%?}"
  duration_unit="${value#"${duration_amount}"}"
  error_message="${variable_name} must be a positive duration ending in s, m, h, or d; got '${value}'."

  if ! is_positive_integer "$duration_amount"; then
    echo "$error_message" >&2
    exit "$CONFIG_ERROR_EXIT_CODE"
  fi

  case "$duration_unit" in
    s|m|h|d) ;;
    *)
      echo "$error_message" >&2
      exit "$CONFIG_ERROR_EXIT_CODE"
      ;;
  esac
}

validate_positive_integer_config "APP_INSIGHTS_TIMEOUT_SECONDS" "$APP_INSIGHTS_TIMEOUT_SECONDS"
validate_positive_integer_config "APP_INSIGHTS_POLL_SECONDS" "$APP_INSIGHTS_POLL_SECONDS"
validate_duration_config "APP_INSIGHTS_LOOKBACK" "$APP_INSIGHTS_LOOKBACK"

require_dependency_telemetry=false
require_trace_telemetry=false

if is_true "$REQUIRE_DEPENDENCY_TELEMETRY"; then
  require_dependency_telemetry=true
fi

if is_true "$REQUIRE_TRACE_TELEMETRY"; then
  require_trace_telemetry=true
fi

if [ -z "$APP_INSIGHTS_REQUEST_URL_CONTAINS" ] && [ -z "$APP_INSIGHTS_ROLE_INSTANCE_CONTAINS" ] \
    && ! is_true "$ALLOW_UNSCOPED_TELEMETRY_CHECK"; then
  echo "Telemetry check requires APP_INSIGHTS_REQUEST_URL_CONTAINS or APP_INSIGHTS_ROLE_INSTANCE_CONTAINS to scope shared App Insights data." >&2
  echo "Set ALLOW_UNSCOPED_TELEMETRY_CHECK=true only for intentional broad role-level checks." >&2
  exit "$CONFIG_ERROR_EXIT_CODE"
fi

if [ "$require_trace_telemetry" = "true" ] && [ -z "$APP_INSIGHTS_TRACE_MARKER" ]; then
  echo "APP_INSIGHTS_TRACE_MARKER is required when REQUIRE_TRACE_TELEMETRY=true." >&2
  exit "$CONFIG_ERROR_EXIT_CODE"
fi

if ! command -v az >/dev/null 2>&1; then
  echo "Azure CLI 'az' is required to query Application Insights."
  exit "$CONFIG_ERROR_EXIT_CODE"
fi

AZURE_ACCOUNT_SUBSCRIPTION_ID=""

if ! AZURE_ACCOUNT_SUBSCRIPTION_ID="$(az account show --query id --output tsv 2>/dev/null)"; then
  if [ -n "${AZURE_CLIENT_ID:-}" ] && [ -n "${AZURE_CLIENT_SECRET:-}" ] && [ -n "${AZURE_TENANT_ID:-}" ]; then
    echo "Azure CLI is not logged in. Logging in with supplied service principal credentials."
    az login \
      --service-principal \
      --username "$AZURE_CLIENT_ID" \
      --password "$AZURE_CLIENT_SECRET" \
      --tenant "$AZURE_TENANT_ID" \
      --output none
  else
    echo "Azure CLI is not logged in or has no active subscription."
    echo "Set AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, and AZURE_TENANT_ID, or run this from an authenticated az session."
    exit "$CONFIG_ERROR_EXIT_CODE"
  fi
fi

if [ -n "${AZURE_SUBSCRIPTION_ID:-}" ]; then
  az account set --subscription "$AZURE_SUBSCRIPTION_ID"
else
  if [ -z "$AZURE_ACCOUNT_SUBSCRIPTION_ID" ]; then
    AZURE_ACCOUNT_SUBSCRIPTION_ID="$(az account show --query id --output tsv)"
  fi
  AZURE_SUBSCRIPTION_ID="$AZURE_ACCOUNT_SUBSCRIPTION_ID"
fi

if [ -n "${APP_INSIGHTS_RESOURCE_ID:-}" ]; then
  app_insights_uri="https://management.azure.com${APP_INSIGHTS_RESOURCE_ID}"
else
  app_insights_uri="https://management.azure.com/subscriptions/${AZURE_SUBSCRIPTION_ID}/resourceGroups/${APP_INSIGHTS_RESOURCE_GROUP}/providers/Microsoft.Insights/components/${APP_INSIGHTS_APP_NAME}"
fi
app_insights_query_uri="${app_insights_uri}/query?api-version=${APP_INSIGHTS_API_VERSION}"

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

kql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

status_for_count() {
  if [ "$1" -lt "$MIN_REQUIRED_TELEMETRY_COUNT" ]; then
    echo "$STATUS_FAIL"
  else
    echo "$STATUS_PASS"
  fi
}

query_telemetry_counts() {
  body="$1"
  error_file="$(mktemp)"

  echo "Querying telemetry..." >&2

  if ! counts=$(az rest \
      --method post \
      --uri "$app_insights_query_uri" \
      --headers "Content-Type=application/json" \
      --body "$body" \
      --query "tables[0].rows[0]" \
      --output tsv 2>"$error_file"); then
    echo "Failed to query telemetry from Application Insights:" >&2
    cat "$error_file" >&2
    rm -f "$error_file"
    exit "$CONFIG_ERROR_EXIT_CODE"
  fi
  rm -f "$error_file"

  set -- $counts
  if [ "$#" -ne "$TELEMETRY_COUNT_COLUMNS" ]; then
    echo "Unexpected telemetry query result:" >&2
    echo "$counts" >&2
    exit "$CONFIG_ERROR_EXIT_CODE"
  fi

  for count in "$@"; do
    case "$count" in
      ''|*[!0-9]*)
        echo "Unexpected telemetry count:" >&2
        echo "$count" >&2
        exit "$CONFIG_ERROR_EXIT_CODE"
        ;;
    esac
  done

  printf '%s %s %s %s %s %s\n' "$1" "$2" "$3" "$4" "$5" "$6"
}

query_telemetry_samples() {
  sample_query="$1"
  escaped_sample_query="$(json_escape "$sample_query")"
  sample_body="{\"query\":\"${escaped_sample_query}\"}"
  error_file="$(mktemp)"

  if ! samples=$(az rest \
      --method post \
      --uri "$app_insights_query_uri" \
      --headers "Content-Type=application/json" \
      --body "$sample_body" \
      --query "tables[0].rows" \
      --output tsv 2>"$error_file"); then
    echo "Failed to query telemetry samples from Application Insights:" >&2
    cat "$error_file" >&2
    rm -f "$error_file"
    return
  fi
  rm -f "$error_file"

  if [ -n "$samples" ]; then
    printf '%s\n' "$samples"
  else
    echo "<no samples>"
  fi
}

role_name="$(kql_escape "$APP_INSIGHTS_ROLE_NAME")"
base_filter="timestamp > ago(${APP_INSIGHTS_LOOKBACK}) | where cloud_RoleName == '${role_name}'"
request_url_filter=""

if [ -n "$APP_INSIGHTS_ROLE_INSTANCE_CONTAINS" ]; then
  role_instance_contains="$(kql_escape "$APP_INSIGHTS_ROLE_INSTANCE_CONTAINS")"
  base_filter="${base_filter} | where cloud_RoleInstance contains '${role_instance_contains}'"
fi

if [ -z "$APP_INSIGHTS_ROLE_INSTANCE_CONTAINS" ] && [ -n "$APP_INSIGHTS_REQUEST_URL_CONTAINS" ]; then
  request_url_contains="$(kql_escape "$APP_INSIGHTS_REQUEST_URL_CONTAINS")"
  request_url_filter=" | where url contains '${request_url_contains}'"
fi

dependency_count_expression="$DISABLED_TELEMETRY_COUNT_EXPRESSION"
role_dependency_count_expression="$DISABLED_TELEMETRY_COUNT_EXPRESSION"
trace_count_expression="$DISABLED_TELEMETRY_COUNT_EXPRESSION"
role_trace_marker_count_expression="$DISABLED_TELEMETRY_COUNT_EXPRESSION"

telemetry_query="let filtered_requests = requests | where ${base_filter};"
telemetry_query="${telemetry_query} let matching_requests = filtered_requests${request_url_filter} | project operation_Id;"
telemetry_query="${telemetry_query} let request_operations = matching_requests | distinct operation_Id;"

if [ "$require_dependency_telemetry" = "true" ]; then
  telemetry_query="${telemetry_query} let filtered_dependencies = dependencies | where ${base_filter};"
  dependency_count_expression="toscalar(filtered_dependencies | where operation_Id in (request_operations) | summarize Count=count())"
  role_dependency_count_expression="toscalar(filtered_dependencies | summarize Count=count())"
fi

if [ "$require_trace_telemetry" = "true" ]; then
  trace_marker="$(kql_escape "$APP_INSIGHTS_TRACE_MARKER")"
  telemetry_query="${telemetry_query} let filtered_traces = traces | where ${base_filter};"
  telemetry_query="${telemetry_query} let filtered_traces_with_marker = filtered_traces | where message contains '${trace_marker}';"
  trace_count_expression="toscalar(filtered_traces_with_marker | where operation_Id in (request_operations) | summarize Count=count())"
  role_trace_marker_count_expression="toscalar(filtered_traces_with_marker | summarize Count=count())"
fi

telemetry_query="${telemetry_query} print RequestCount=toscalar(matching_requests | summarize Count=count()), RoleRequestCount=toscalar(filtered_requests | summarize Count=count()), DependencyCount=${dependency_count_expression}, RoleDependencyCount=${role_dependency_count_expression}, TraceCount=${trace_count_expression}, RoleTraceMarkerCount=${role_trace_marker_count_expression}"
escaped_telemetry_query="$(json_escape "$telemetry_query")"
telemetry_query_body="{\"query\":\"${escaped_telemetry_query}\"}"

sample_filter="timestamp > ago(${APP_INSIGHTS_LOOKBACK}) | where cloud_RoleName == '${role_name}'"
telemetry_sample_query="requests | where ${sample_filter} | summarize Count=count() by cloud_RoleInstance, url | order by Count desc | take 10"

deadline=$(( $(date +%s) + APP_INSIGHTS_TIMEOUT_SECONDS ))

echo "Checking Application Insights telemetry"
echo "  app: ${APP_INSIGHTS_APP_NAME}"
echo "  resource group: ${APP_INSIGHTS_RESOURCE_GROUP}"
echo "  resource id: ${APP_INSIGHTS_RESOURCE_ID:-}"
echo "  cloud role: ${APP_INSIGHTS_ROLE_NAME}"
echo "  role instance contains: ${APP_INSIGHTS_ROLE_INSTANCE_CONTAINS:-<not set>}"
echo "  source env: ${APP_INSIGHTS_SOURCE_ENV}"
echo "  request URL contains: ${APP_INSIGHTS_REQUEST_URL_CONTAINS:-<not set>}"
echo "  marker: ${APP_INSIGHTS_TRACE_MARKER:-<not required>}"
echo "  lookback: ${APP_INSIGHTS_LOOKBACK}"
echo "  subscription: ${AZURE_SUBSCRIPTION_ID}"
echo "  required: requests=true, dependencies=${REQUIRE_DEPENDENCY_TELEMETRY}, traces=${REQUIRE_TRACE_TELEMETRY}"
if [ "$require_dependency_telemetry" = "true" ] || [ "$require_trace_telemetry" = "true" ]; then
  echo "  dependency/trace scope: correlated by operation_Id to matching request telemetry"
fi

while true; do
  echo "Application Insights telemetry check attempt ${attempt}"

  counts="$(query_telemetry_counts "$telemetry_query_body")"
  set -- $counts
  # KQL returns scoped counts for pass/fail plus role-wide counts for diagnostics.
  request_count="$1"
  role_request_count="$2"
  dependency_count="$3"
  role_dependency_count="$4"
  trace_count="$5"
  role_trace_marker_count="$6"

  request_status="$(status_for_count "$request_count")"
  dependency_status="$STATUS_SKIP"
  trace_status="$STATUS_SKIP"

  if [ "$require_dependency_telemetry" = "true" ]; then
    dependency_status="$(status_for_count "$dependency_count")"
  fi

  if [ "$require_trace_telemetry" = "true" ]; then
    trace_status="$(status_for_count "$trace_count")"
  fi

  echo "Telemetry result: requests=${request_status} (${request_count}), dependencies=${dependency_status} (${dependency_count}), traces=${trace_status} (${trace_count})"
  diagnostics="Telemetry diagnostics: role_requests=${role_request_count}"
  if [ "$require_dependency_telemetry" = "true" ]; then
    diagnostics="${diagnostics}, role_dependencies=${role_dependency_count}"
  fi
  if [ "$require_trace_telemetry" = "true" ]; then
    diagnostics="${diagnostics}, role_traces_with_marker=${role_trace_marker_count}"
  fi
  echo "$diagnostics"

  passed=true
  if [ "$request_status" = "$STATUS_FAIL" ] || [ "$dependency_status" = "$STATUS_FAIL" ] || [ "$trace_status" = "$STATUS_FAIL" ]; then
    passed=false
  fi

  if [ "$passed" = "true" ]; then
    echo "Application Insights telemetry check PASSED."
    exit 0
  fi

  if [ "$(date +%s)" -ge "$deadline" ]; then
    echo "Application Insights telemetry check FAILED before timeout."
    echo "Missing required telemetry:"
    [ "$request_status" = "$STATUS_FAIL" ] && echo "  - request telemetry for cloud role '${APP_INSIGHTS_ROLE_NAME}'"
    [ "$dependency_status" = "$STATUS_FAIL" ] && echo "  - dependency telemetry correlated with matching request telemetry"
    [ "$trace_status" = "$STATUS_FAIL" ] && echo "  - trace telemetry containing '${APP_INSIGHTS_TRACE_MARKER}' and correlated with matching request telemetry"
    echo "Diagnostic counts:"
    echo "  - role request telemetry: ${role_request_count}"
    echo "  - matching request telemetry: ${request_count}"
    if [ "$require_dependency_telemetry" = "true" ]; then
      echo "  - role dependency telemetry: ${role_dependency_count}"
      echo "  - correlated dependency telemetry: ${dependency_count}"
    fi
    if [ "$require_trace_telemetry" = "true" ]; then
      echo "  - role trace telemetry containing '${APP_INSIGHTS_TRACE_MARKER}': ${role_trace_marker_count}"
      echo "  - correlated trace telemetry containing '${APP_INSIGHTS_TRACE_MARKER}': ${trace_count}"
    fi
    echo "Sample request telemetry for role '${APP_INSIGHTS_ROLE_NAME}' within ${APP_INSIGHTS_LOOKBACK}:"
    query_telemetry_samples "$telemetry_sample_query"
    exit "$TELEMETRY_FAILURE_EXIT_CODE"
  fi

  echo "Telemetry not complete yet. Waiting ${APP_INSIGHTS_POLL_SECONDS}s for App Insights ingestion..."
  attempt=$((attempt + 1))
  sleep "$APP_INSIGHTS_POLL_SECONDS"
done
