#!/usr/bin/env bash
# Runs only from an explicitly enabled, isolated PR-preview Jenkins build.
set -euo pipefail

: "${LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE:?Set the case reference created by the outage smoke}"
: "${LOGSTASH_MANUAL_REQUEUE_CASE_TYPE:?Set the case type, for example AAT_PRIVATE}"
: "${BRANCH_NAME:?Jenkins BRANCH_NAME is required}"
: "${TEAM_NAMESPACE:?Jenkins TEAM_NAMESPACE is required}"

case "${BRANCH_NAME}" in PR*) ;; *) echo "Refusing to run outside a PR preview." >&2; exit 1;; esac
case "${TEAM_NAMESPACE}" in pr-*) ;; *) echo "Refusing non-PR namespace: ${TEAM_NAMESPACE}" >&2; exit 1;; esac
[[ "${LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE}" =~ ^[0-9]+$ ]] || {
  echo "Case reference must be numeric." >&2; exit 1;
}
[[ "${LOGSTASH_MANUAL_REQUEUE_CASE_TYPE}" =~ ^[A-Za-z0-9_]+$ ]] || {
  echo "Case type contains unsupported characters." >&2; exit 1;
}

release="ccd-data-store-api-$(tr '[:upper:]' '[:lower:]' <<<"${BRANCH_NAME}")"
postgres_pod="$(kubectl get pods -n "${TEAM_NAMESPACE}" \
  -l "app.kubernetes.io/instance=${release},app.kubernetes.io/name=postgresql" \
  -o jsonpath='{.items[0].metadata.name}')"
es_pod="$(kubectl get pods -n "${TEAM_NAMESPACE}" \
  -l "app.kubernetes.io/instance=${release},app.kubernetes.io/name=elasticsearch" \
  -o jsonpath='{.items[0].metadata.name}')"

[[ -n "${postgres_pod}" && -n "${es_pod}" ]] || {
  echo "Could not find the preview PostgreSQL or Elasticsearch pod." >&2; exit 1;
}

psql() {
  kubectl exec -n "${TEAM_NAMESPACE}" "${postgres_pod}" -c postgresql -- \
    env PGPASSWORD=javapassword psql -v ON_ERROR_STOP=1 -U javapostgres -d data-store -Atqc "$1"
}

case_data_id="$(psql "SELECT id FROM case_data WHERE reference = ${LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE};")"
[[ "${case_data_id}" =~ ^[0-9]+$ ]] || {
  echo "No unique case_data row found for reference ${LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE}." >&2; exit 1;
}

psql "INSERT INTO case_data_logstash_queue (case_data_id)
SELECT id FROM case_data cd
WHERE cd.id = ${case_data_id}
  AND NOT EXISTS (
    SELECT 1 FROM case_data_logstash_queue q WHERE q.case_data_id = cd.id
  );"

for _ in {1..90}; do
  remaining="$(psql "SELECT count(*) FROM case_data_logstash_queue WHERE case_data_id = ${case_data_id};")"
  [[ "${remaining}" == "0" ]] && break
  sleep 1
done
[[ "${remaining}" == "0" ]] || {
  echo "Re-queued row was not consumed within 90 seconds." >&2; exit 1;
}

index="$(tr '[:upper:]' '[:lower:]' <<<"${LOGSTASH_MANUAL_REQUEUE_CASE_TYPE}")_cases"
document="$(kubectl exec -n "${TEAM_NAMESPACE}" "${es_pod}" -- \
  curl --fail --silent "http://${release}-es-master:9200/${index}/_doc/${case_data_id}")"

if [[ -n "${LOGSTASH_MANUAL_REQUEUE_EXPECTED_SUPPLEMENTARY_DATA:-}" ]]; then
  jq -e --argjson expected "${LOGSTASH_MANUAL_REQUEUE_EXPECTED_SUPPLEMENTARY_DATA}" \
    '.found == true and ._source.supplementary_data == $expected' <<<"${document}" >/dev/null
else
  jq -e '.found == true and ._source.supplementary_data != null' <<<"${document}" >/dev/null
fi

echo "Manual requeue smoke passed for case ${LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE}."
