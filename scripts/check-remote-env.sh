#!/usr/bin/env bash
set -u

env_name="${1:-aat}"
env_file=".${env_name}-remote-env"

case "${env_name}" in
  aat|AAT) run_task="runRemoteAAT" ;;
  demo|Demo) run_task="runRemoteDemo" ;;
  *) run_task="runRemote$(printf '%s' "${env_name}" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')" ;;
esac

if [ ! -f "${env_file}" ]; then
  echo "ERROR: ${env_file} does not exist." >&2
  echo "Create it first: ./gradlew reloadEnvSecrets -Penv=${env_name}" >&2
  exit 1
fi

load_env_file() {
  while IFS= read -r line || [ -n "${line}" ]; do
    case "${line}" in
      ''|\#*) continue ;;
    esac

    key="${line%%=*}"
    value="${line#*=}"

    if [ "${key}" = "${line}" ]; then
      echo "WARN ${env_file}: ignoring line without '='." >&2
      continue
    fi

    case "${key}" in
      ''|*[!A-Za-z0-9_]*)
        echo "WARN ${env_file}: ignoring invalid key '${key}'." >&2
        continue
        ;;
    esac

    export "${key}=${value}"
  done < "${env_file}"
}

apply_alias() {
  local alias="$1"
  local source="$2"
  eval "source_value=\${${source}:-}"

  if [ -n "${source_value}" ]; then
    export "${alias}=${source_value}"
  fi
}

unset DATA_STORE_DB_HOST DATA_STORE_DB_PORT
unset IDAM_S2S_URL S2S_URL_BASE
unset IDAM_API_BASE_URL IDAM_API_URL_BASE
unset IDAM_OIDC_URL OIDC_ISSUER
unset DEFINITION_STORE_HOST DEFINITION_STORE_URL_BASE
unset USER_PROFILE_HOST CASE_DOCUMENT_AM_URL
unset ROLE_ASSIGNMENT_URL ROLE_ASSIGNMENT_HOST
unset RD_LOCATION_REF_API_BASE_URL
unset ELASTIC_SEARCH_HOSTS ELASTIC_SEARCH_DATA_NODES_HOSTS ELASTIC_SEARCH_DATA_NODES_URL

load_env_file
apply_alias "IDAM_API_URL_BASE" "IDAM_API_BASE_URL"
apply_alias "S2S_URL_BASE" "IDAM_S2S_URL"
apply_alias "DEFINITION_STORE_URL_BASE" "DEFINITION_STORE_HOST"
apply_alias "ROLE_ASSIGNMENT_HOST" "ROLE_ASSIGNMENT_URL"
apply_alias "TEST_URL" "CCD_DATA_STORE_API_BASE_URL"

if [ -n "${AAT_DEFINITION_STORE_HOST:-}" ]; then
  export DEFINITION_STORE_HOST="${AAT_DEFINITION_STORE_HOST}"
  export DEFINITION_STORE_URL_BASE="${AAT_DEFINITION_STORE_HOST}"
fi

status=0

is_aat_env() {
  case "${env_name}" in
    aat|AAT)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

clean_url() {
  printf '%s' "$1" | sed -E 's/^[[:space:]]*//; s/[[:space:]]*$//; s/^"//; s/"$//'
}

host_from_url() {
  clean_url "$1" | sed -E 's#^[a-zA-Z][a-zA-Z0-9+.-]*://##; s#/.*$##; s#:.*$##'
}

port_from_url() {
  local url="$1"
  local default_port="$2"
  local host_port
  host_port="$(clean_url "${url}" | sed -E 's#^[a-zA-Z][a-zA-Z0-9+.-]*://##; s#/.*$##')"
  case "${host_port}" in
    *:*) printf '%s' "${host_port##*:}" ;;
    *) printf '%s' "${default_port}" ;;
  esac
}

can_resolve() {
  local host="$1"

  if command -v dscacheutil >/dev/null 2>&1 && dscacheutil -q host -a name "${host}" >/dev/null 2>&1; then
    return 0
  fi
  if command -v getent >/dev/null 2>&1 && getent hosts "${host}" >/dev/null 2>&1; then
    return 0
  fi
  if command -v nslookup >/dev/null 2>&1 && nslookup "${host}" >/dev/null 2>&1; then
    return 0
  fi

  return 1
}

print_dns_hint() {
  local host="$1"

  case "${host}" in
    host.docker.internal)
      echo "      host.docker.internal is only for a process running inside Docker that must call a service on the host machine." >&2
      ;;
    *.service.core-compute-aat.internal|*.internal)
      echo "      ${host} is an AAT internal hostname. Connect to the required HMCTS VPN/bastion/private DNS route, or override/tunnel this dependency before running ${run_task}." >&2
      ;;
  esac
}

check_aat_s2s_url() {
  local url="$1"
  local host

  [ -n "${url}" ] || return
  is_aat_env || return

  host="$(host_from_url "${url}")"
  case "${host}" in
    rpe-service-auth-provider-aat.service.core-compute-aat.internal|localhost|127.0.0.1)
      return 0
      ;;
    host.docker.internal)
      echo "FAIL s2s: AAT IDAM_S2S_URL/S2S_URL_BASE uses ${url}" >&2
      echo "      For normal local Gradle ${run_task}, use http://rpe-service-auth-provider-aat.service.core-compute-aat.internal." >&2
      echo "      If using a host S2S tunnel from a Mac app process, use http://localhost:<port>." >&2
      echo "      Use host.docker.internal only for a process running inside Docker that calls a host-machine tunnel." >&2
      ;;
    *)
      echo "FAIL s2s: AAT IDAM_S2S_URL/S2S_URL_BASE uses unexpected host '${host}' from ${url}" >&2
      echo "      Expected http://rpe-service-auth-provider-aat.service.core-compute-aat.internal, or http://localhost:<port> for an intentional host S2S tunnel." >&2
      ;;
  esac

  status=1
  return 1
}

check_aat_elastic_url() {
  local url="$1"
  local host

  [ -n "${url}" ] || return
  is_aat_env || return

  host="$(host_from_url "${url}")"
  case "${host}" in
    ccd-data-*.service.core-compute-aat.internal|localhost|127.0.0.1)
      return 0
      ;;
    host.docker.internal)
      echo "FAIL elastic search: AAT ELASTIC_SEARCH_HOSTS/ELASTIC_SEARCH_DATA_NODES_HOSTS uses ${url}" >&2
      echo "      For normal local Gradle ${run_task}, use http://ccd-data-<n>.service.core-compute-aat.internal:9200." >&2
      echo "      If using a host Elasticsearch tunnel from a Mac app process, use http://localhost:<port>." >&2
      echo "      Use host.docker.internal only for a process running inside Docker that calls a host-machine tunnel." >&2
      ;;
    *)
      echo "FAIL elastic search: AAT ELASTIC_SEARCH_HOSTS/ELASTIC_SEARCH_DATA_NODES_HOSTS uses unexpected host '${host}' from ${url}" >&2
      echo "      Expected http://ccd-data-<n>.service.core-compute-aat.internal:9200, or http://localhost:<port> for an intentional host Elasticsearch tunnel." >&2
      ;;
  esac

  status=1
  return 1
}

check_tcp() {
  local label="$1"
  local host="$2"
  local port="$3"

  if [ -z "${host}" ] || [ -z "${port}" ]; then
    echo "FAIL ${label}: missing host or port" >&2
    status=1
    return
  fi

  if ! can_resolve "${host}"; then
    echo "FAIL ${label}: cannot resolve ${host}" >&2
    print_dns_hint "${host}"
    status=1
    return
  fi

  if nc -G 5 -z "${host}" "${port}" >/dev/null 2>&1 || nc -w 5 -z "${host}" "${port}" >/dev/null 2>&1; then
    echo "OK   ${label}: ${host}:${port}"
  else
    echo "FAIL ${label}: cannot connect to ${host}:${port}" >&2
    status=1
  fi
}

check_url() {
  local label="$1"
  local url="$2"
  local default_port="$3"

  [ -n "${url}" ] || return
  check_tcp "${label}" "$(host_from_url "${url}")" "$(port_from_url "${url}" "${default_port}")"
}

check_url_list() {
  local label="$1"
  local urls="$2"
  local default_port="$3"
  local index=0
  local url

  [ -n "${urls}" ] || return

  while IFS= read -r url; do
    url="$(clean_url "${url}")"
    [ -n "${url}" ] || continue
    check_url "${label}[${index}]" "${url}" "${default_port}"
    index=$((index + 1))
  done < <(printf '%s' "${urls}" | tr ',' '\n')
}

check_elastic_url_list() {
  local label="$1"
  local urls="$2"
  local default_port="$3"
  local index=0
  local url

  [ -n "${urls}" ] || return

  while IFS= read -r url; do
    url="$(clean_url "${url}")"
    [ -n "${url}" ] || continue
    if check_aat_elastic_url "${url}"; then
      check_url "${label}[${index}]" "${url}" "${default_port}"
    fi
    index=$((index + 1))
  done < <(printf '%s' "${urls}" | tr ',' '\n')
}

check_health() {
  local label="$1"
  local url="$2"
  local health_url

  [ -n "${url}" ] || return

  if ! command -v curl >/dev/null 2>&1; then
    echo "WARN ${label}: curl not available; skipping health check"
    return
  fi

  health_url="${url%/}/health"
  if curl --fail --silent --show-error --max-time 10 "${health_url}" >/dev/null; then
    echo "OK   ${label} health: ${health_url}"
  else
    echo "FAIL ${label} health: ${health_url}" >&2
    status=1
  fi
}

s2s_url="${IDAM_S2S_URL:-${S2S_URL_BASE:-}}"
check_tcp "database" "${DATA_STORE_DB_HOST:-}" "${DATA_STORE_DB_PORT:-5432}"
if check_aat_s2s_url "${s2s_url}"; then
  check_url "s2s" "${s2s_url}" 80
fi
check_url "idam api" "${IDAM_API_BASE_URL:-${IDAM_API_URL_BASE:-}}" 443
check_url "idam oidc" "${IDAM_OIDC_URL:-${OIDC_ISSUER:-}}" 443
check_url "definition store" "${DEFINITION_STORE_HOST:-}" 80
if [ -n "${AAT_DEFINITION_STORE_HOST:-}" ]; then
  check_health "definition store" "${DEFINITION_STORE_HOST:-}"
fi
check_url "user profile" "${USER_PROFILE_HOST:-}" 80
check_url "case document" "${CASE_DOCUMENT_AM_URL:-}" 80
check_url "role assignment" "${ROLE_ASSIGNMENT_URL:-${ROLE_ASSIGNMENT_HOST:-}}" 80
check_url "location reference" "${RD_LOCATION_REF_API_BASE_URL:-}" 80
check_elastic_url_list "elastic search" "${ELASTIC_SEARCH_HOSTS:-${ELASTIC_SEARCH_DATA_NODES_HOSTS:-${ELASTIC_SEARCH_DATA_NODES_URL:-}}}" 9200

if [ "${status}" -ne 0 ]; then
  echo
  echo "One or more remote dependencies are unreachable. Check VPN/bastion/DNS before running ${run_task}." >&2
fi

exit "${status}"
