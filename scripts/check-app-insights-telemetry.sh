#!/usr/bin/env sh

set -eu

APP_INSIGHTS_ENV="${APP_INSIGHTS_ENV:-aat}"
APP_INSIGHTS_APP_NAME="${APP_INSIGHTS_APP_NAME:-${APP_INSIGHTS_APP:-ccd-${APP_INSIGHTS_ENV}}}"
APP_INSIGHTS_RESOURCE_GROUP="${APP_INSIGHTS_RESOURCE_GROUP:-ccd-shared-${APP_INSIGHTS_ENV}}"
APP_INSIGHTS_ROLE_NAME="${APP_INSIGHTS_ROLE_NAME:-ccd-data-store-api}"
APP_INSIGHTS_LOOKBACK="${APP_INSIGHTS_LOOKBACK:-${APP_INSIGHTS_TELEMETRY_LOOKBACK:-2h}}"
APP_INSIGHTS_TIMEOUT_SECONDS="${APP_INSIGHTS_TIMEOUT_SECONDS:-600}"
APP_INSIGHTS_POLL_SECONDS="${APP_INSIGHTS_POLL_SECONDS:-30}"
REQUIRE_DEPENDENCY_TELEMETRY="${REQUIRE_DEPENDENCY_TELEMETRY:-true}"
REQUIRE_TRACE_TELEMETRY="${REQUIRE_TRACE_TELEMETRY:-true}"
attempt=1

if ! command -v az >/dev/null 2>&1; then
  echo "Azure CLI 'az' is required to query Application Insights."
  exit 2
fi

if ! az account show >/dev/null 2>&1; then
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
    exit 2
  fi
fi

if [ -n "${AZURE_SUBSCRIPTION_ID:-}" ]; then
  az account set --subscription "$AZURE_SUBSCRIPTION_ID"
else
  AZURE_SUBSCRIPTION_ID="$(az account show --query id --output tsv)"
fi

is_true() {
  case "$1" in
    true|TRUE|True|1|yes|YES|Yes) return 0 ;;
    *) return 1 ;;
  esac
}

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

kql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

resolve_trace_marker() {
  if [ -n "${APP_INSIGHTS_TRACE_MARKER:-}" ]; then
    printf '%s' "$APP_INSIGHTS_TRACE_MARKER"
    return
  fi

  if [ -x ./gradlew ]; then
    if marker="$(./gradlew -q printAuditLogFormatterTag 2>/dev/null | awk 'NF { value = $0 } END { print value }')" \
        && [ -n "$marker" ]; then
      printf '%s' "$marker"
      return
    fi
  fi

  echo "APP_INSIGHTS_TRACE_MARKER is not set and AuditLogFormatter.TAG could not be resolved." >&2
  exit 2
}

query_count() {
  label="$1"
  query="$2"
  escaped_query="$(json_escape "$query")"
  body="{\"query\":\"${escaped_query}\"}"

  echo "Querying ${label} telemetry..." >&2

  if [ -n "${APP_INSIGHTS_RESOURCE_ID:-}" ]; then
    uri="https://management.azure.com${APP_INSIGHTS_RESOURCE_ID}/query?api-version=2018-04-20"
  else
    uri="https://management.azure.com/subscriptions/${AZURE_SUBSCRIPTION_ID}/resourceGroups/${APP_INSIGHTS_RESOURCE_GROUP}/providers/Microsoft.Insights/components/${APP_INSIGHTS_APP_NAME}/query?api-version=2018-04-20"
  fi

  if ! count=$(az rest \
      --method post \
      --uri "$uri" \
      --headers "Content-Type=application/json" \
      --body "$body" \
      --query "tables[0].rows[0][0]" \
      --output tsv 2>&1); then
    echo "Failed to query ${label} telemetry from Application Insights:"
    echo "$count"
    exit 2
  fi

  if [ -z "$count" ]; then
    count=0
  fi

  case "$count" in
    *[!0-9]*)
      echo "Unexpected ${label} telemetry query result:"
      echo "$count"
      exit 2
      ;;
  esac

  echo "$count"
}

APP_INSIGHTS_TRACE_MARKER="$(resolve_trace_marker)"
role_name="$(kql_escape "$APP_INSIGHTS_ROLE_NAME")"
trace_marker="$(kql_escape "$APP_INSIGHTS_TRACE_MARKER")"
lookback="$APP_INSIGHTS_LOOKBACK"
base_filter="timestamp > ago(${lookback}) | where cloud_RoleName == '${role_name}'"
requests_query="requests | where ${base_filter} | summarize Count=count()"
dependencies_query="dependencies | where ${base_filter} | summarize Count=count()"
traces_query="traces | where ${base_filter} | where message contains '${trace_marker}' | summarize Count=count()"

deadline=$(( $(date +%s) + APP_INSIGHTS_TIMEOUT_SECONDS ))

echo "Checking Application Insights telemetry"
echo "  app: ${APP_INSIGHTS_APP_NAME}"
echo "  resource group: ${APP_INSIGHTS_RESOURCE_GROUP}"
echo "  resource id: ${APP_INSIGHTS_RESOURCE_ID:-}"
echo "  cloud role: ${APP_INSIGHTS_ROLE_NAME}"
echo "  marker: ${APP_INSIGHTS_TRACE_MARKER}"
echo "  lookback: ${APP_INSIGHTS_LOOKBACK}"
echo "  subscription: ${AZURE_SUBSCRIPTION_ID}"
echo "  required: requests=true, dependencies=${REQUIRE_DEPENDENCY_TELEMETRY}, traces=${REQUIRE_TRACE_TELEMETRY}"

while true; do
  echo "Application Insights telemetry check attempt ${attempt}"

  request_count=$(query_count "request" "$requests_query")
  dependency_count=$(query_count "dependency" "$dependencies_query")
  trace_count=$(query_count "trace" "$traces_query")

  request_status="PASS"
  dependency_status="SKIP"
  trace_status="SKIP"

  if [ "$request_count" -lt 1 ]; then
    request_status="FAIL"
  fi

  if is_true "$REQUIRE_DEPENDENCY_TELEMETRY"; then
    dependency_status="PASS"
    if [ "$dependency_count" -lt 1 ]; then
      dependency_status="FAIL"
    fi
  fi

  if is_true "$REQUIRE_TRACE_TELEMETRY"; then
    trace_status="PASS"
    if [ "$trace_count" -lt 1 ]; then
      trace_status="FAIL"
    fi
  fi

  echo "Telemetry result: requests=${request_status} (${request_count}), dependencies=${dependency_status} (${dependency_count}), traces=${trace_status} (${trace_count})"

  passed=true

  if [ "$request_status" = "FAIL" ]; then
    passed=false
  fi

  if [ "$dependency_status" = "FAIL" ]; then
    passed=false
  fi

  if [ "$trace_status" = "FAIL" ]; then
    passed=false
  fi

  if [ "$passed" = "true" ]; then
    echo "Application Insights telemetry check PASSED."
    exit 0
  fi

  if [ "$(date +%s)" -ge "$deadline" ]; then
    echo "Application Insights telemetry check FAILED before timeout."
    echo "Missing required telemetry:"
    [ "$request_status" = "FAIL" ] && echo "  - request telemetry for cloud role '${APP_INSIGHTS_ROLE_NAME}'"
    [ "$dependency_status" = "FAIL" ] && echo "  - dependency telemetry for cloud role '${APP_INSIGHTS_ROLE_NAME}'"
    [ "$trace_status" = "FAIL" ] && echo "  - trace telemetry containing '${APP_INSIGHTS_TRACE_MARKER}'"
    exit 1
  fi

  echo "Telemetry not complete yet. Waiting ${APP_INSIGHTS_POLL_SECONDS}s for App Insights ingestion..."
  attempt=$((attempt + 1))
  sleep "$APP_INSIGHTS_POLL_SECONDS"
done
