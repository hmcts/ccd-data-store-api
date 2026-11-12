#!/usr/bin/env bash
# Temporary, read-only diagnostic for the PR definition-store database.
set -euo pipefail
set +x

namespace=${SCHEMA_CHECK_NAMESPACE:?Missing namespace}
release=${SCHEMA_CHECK_RELEASE:?Missing release}
[[ "$release" =~ ^ccd-data-store-api-pr-[0-9]+$ ]] || {
    echo 'Schema check is restricted to data-store PR releases.' >&2
    exit 1
}
diagnostics=definition-store-schema-diagnostics
mkdir -p "$diagnostics"
kube=(kubectl --namespace "$namespace" --request-timeout=20s)

save_pod_status() {
    # Exclude pod specs, which may contain credentials in environment values.
    "${kube[@]}" get pods -o json | jq --arg release "$release" '
        [.items[] | select(.metadata.name | startswith($release + "-"))
         | select(.metadata.name | test("postgresql|ccd-definition-store"))
         | {name: .metadata.name, created: .metadata.creationTimestamp,
            phase: .status.phase, containers: [.status.containerStatuses[]?
            | {name, image, imageID, restartCount, state, lastState}]}]
    ' > "$diagnostics/pod-status.json"
}

collect_failure_details() {
    local status=$?
    trap - EXIT
    if (( status != 0 )); then
        echo "Definition-store schema check failed; see $diagnostics artifacts." >&2
        if [[ -f "$diagnostics/schema-check.txt" ]]; then
            cat "$diagnostics/schema-check.txt" >&2
        fi
        # Capture only this release's database and definition-store pods.
        save_pod_status || true
        if [[ -s "$diagnostics/pod-status.json" ]]; then
            while IFS= read -r pod; do
                "${kube[@]}" logs "$pod" --all-containers=true --timestamps=true --tail=2000 \
                    > "$diagnostics/$pod.log" 2>&1 || true
                "${kube[@]}" get events --field-selector "involvedObject.name=$pod" \
                    > "$diagnostics/$pod.events.txt" 2>&1 || true
            done < <(jq -r '.[].name' "$diagnostics/pod-status.json" 2>/dev/null)
        fi
    fi
    exit "$status"
}
trap collect_failure_details EXIT

save_pod_status
db_pod=$(jq -er --arg prefix "$release-postgresql-" '
    [.[] | select(.name | startswith($prefix))
     | select(.phase == "Running") | .name]
    | if length == 1 then .[0] else error("Expected one running PostgreSQL pod") end
' "$diagnostics/pod-status.json")

"${kube[@]}" exec -i "$db_pod" -- sh -s > "$diagnostics/schema-check.txt" 2>&1 <<'CHECK_SQL'
set -eu
set +x
export PGHOST=127.0.0.1 PGPORT=5432 PGDATABASE=definition-store PGCONNECT_TIMEOUT=10
export PGUSER="${POSTGRESQL_USERNAME:-${POSTGRES_USER:-javapostgres}}"
export PGPASSWORD="${POSTGRESQL_PASSWORD:-${POSTGRES_PASSWORD:-}}"
password_file=${POSTGRESQL_PASSWORD_FILE:-${POSTGRES_PASSWORD_FILE:-}}
if [ -n "$password_file" ]; then
    PGPASSWORD=$(cat "$password_file")
    export PGPASSWORD
fi
export PGOPTIONS='-c default_transaction_read_only=on -c statement_timeout=15000'

psql -X -v ON_ERROR_STOP=1 -c 'SELECT current_database(), current_user;'
tables_ready=$(psql -X -v ON_ERROR_STOP=1 -Atc "SELECT
    to_regclass('public.role') IS NOT NULL
    AND to_regclass('public.flyway_schema_history') IS NOT NULL;")
if [ "$tables_ready" != t ]; then
    echo 'Required role or flyway_schema_history table is missing.'
    exit 1
fi
psql -X -v ON_ERROR_STOP=1 -c 'SELECT installed_rank, version, description, type, installed_on, success
    FROM public.flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;'
migrations_ok=$(psql -X -v ON_ERROR_STOP=1 -Atc 'SELECT count(*) > 0 AND bool_and(success)
    FROM public.flyway_schema_history;')
if [ "$migrations_ok" != t ]; then
    echo 'Flyway history is empty or contains failed migrations.'
    exit 1
fi
echo 'Definition-store role table and successful migration history verified.'
CHECK_SQL

cat "$diagnostics/schema-check.txt"
