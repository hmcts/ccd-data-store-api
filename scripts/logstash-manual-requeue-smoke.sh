#!/usr/bin/env bash
# Runs only from an explicitly enabled, isolated PR-preview Jenkins build.
set -euo pipefail

: "${LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE:?Set the case reference created by the outage smoke}"
: "${LOGSTASH_MANUAL_REQUEUE_CASE_TYPE:?Set the case type, for example AAT_PRIVATE}"
: "${LOGSTASH_MANUAL_REQUEUE_EXPECTED_SUPPLEMENTARY_DATA:?Set the exact expected supplementary data JSON}"
smoke_mode="${LOGSTASH_SMOKE_MODE:-recovery}"
case "${smoke_mode}" in
  recovery) : "${LOGSTASH_MANUAL_REQUEUE_OUTAGE_EVIDENCE:?Set a link to the recorded outage and Logstash failure evidence}" ;;
  cutover)
    : "${LOGSTASH_CUTOVER_EVIDENCE:?Set a link to legacy ES versions and pre-migration backlog evidence}"
    : "${LOGSTASH_CUTOVER_QUEUE_ID:?Set the migrated backlog ID captured before starting consumers}"
    ;;
  *) echo "Unsupported smoke mode: ${smoke_mode}" >&2; exit 1 ;;
esac
: "${BRANCH_NAME:?Jenkins BRANCH_NAME is required}"
: "${TEAM_NAMESPACE:?Jenkins TEAM_NAMESPACE is required}"

case "${BRANCH_NAME}" in PR*) ;; *) echo "Refusing to run outside a PR preview." >&2; exit 1;; esac
case "${TEAM_NAMESPACE}" in
  pr-*) ;;
  ccd)
    preview_context="$(kubectl config current-context)"
    case "${preview_context}" in
      cft-preview-*-aks) ;;
      *) echo "Shared ccd namespace requires a CFT preview cluster." >&2; exit 1 ;;
    esac
    ;;
  *) echo "Refusing non-preview namespace: ${TEAM_NAMESPACE}" >&2; exit 1 ;;
esac
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

if [[ "${smoke_mode}" == "cutover" ]]; then
  queue_id="${LOGSTASH_CUTOVER_QUEUE_ID}"
  [[ "${queue_id}" =~ ^[0-9]+$ ]] && (( queue_id > 10000000000 )) || {
    echo "Cutover queue ID must exceed 10^10." >&2; exit 1;
  }
  [[ "$(psql "SELECT count(*) FROM flyway_schema_history WHERE success
    AND version IN ('20260917.0001', '20260918.0000', '20260923.0000');")" == "3" ]] || {
    echo "Required cutover migrations have not all succeeded." >&2; exit 1;
  }
else
queue_id="$(psql "INSERT INTO case_data_logstash_queue (case_data_id)
SELECT id FROM case_data cd
WHERE cd.id = ${case_data_id}
  AND NOT EXISTS (
    SELECT 1 FROM case_data_logstash_queue q WHERE q.case_data_id = cd.id
  )
ON CONFLICT (case_data_id) DO NOTHING
RETURNING id;")"
[[ "${queue_id}" =~ ^[0-9]+$ ]] || {
  echo "No queue row was inserted; the case is already queued or was updated concurrently." >&2; exit 1;
}

fi

for _ in {1..90}; do
  remaining="$(psql "SELECT count(*) FROM case_data_logstash_queue WHERE case_data_id = ${case_data_id};")"
  [[ "${remaining}" == "0" ]] && break
  sleep 1
done
[[ "${remaining}" == "0" ]] || {
  echo "Re-queued row was not consumed within 90 seconds." >&2; exit 1;
}

index="$(tr '[:upper:]' '[:lower:]' <<<"${LOGSTASH_MANUAL_REQUEUE_CASE_TYPE}")_cases"
jq -e 'type == "object"' <<<"${LOGSTASH_MANUAL_REQUEUE_EXPECTED_SUPPLEMENTARY_DATA}" >/dev/null
global_case="$(psql "SELECT (data->'SearchCriteria' IS NOT NULL)::int FROM case_data WHERE id = ${case_data_id};")"
if [[ "${smoke_mode}" == "cutover" && "${global_case}" != "1" ]]; then
  echo "Cutover smoke requires a case with SearchCriteria to verify both destinations." >&2; exit 1;
fi
indices=("${index}")
[[ "${global_case}" != "1" ]] || indices+=("global_search")
for destination in "${indices[@]}"; do
  expected="${LOGSTASH_MANUAL_REQUEUE_EXPECTED_SUPPLEMENTARY_DATA}"
  if [[ "${destination}" == "global_search" ]]; then
    expected="$(jq -ce '{HMCTSServiceId: .HMCTSServiceId} | select(.HMCTSServiceId != null)' <<<"${expected}")"
  fi
  verified=false
  for _ in {1..90}; do
    if document="$(kubectl exec -n "${TEAM_NAMESPACE}" "${es_pod}" -- \
      curl --fail --silent --max-time 5 "http://${release}-es-master:9200/${destination}/_doc/${case_data_id}")" \
      && jq -e --argjson version "${queue_id}" --argjson expected "${expected}" \
        '.found == true and ._version == $version and ._source.supplementary_data == $expected' \
        <<<"${document}" >/dev/null; then
      verified=true
      break
    fi
    sleep 1
  done
  [[ "${verified}" == "true" ]] || {
    echo "Expected version and data were not indexed in ${destination} within 90 attempts." >&2; exit 1;
  }
  if [[ "${smoke_mode}" == "cutover" ]]; then
    # A stale replay must be rejected without changing the indexed document.
    before="${document}"
    stale_version=$((queue_id - 1))
    stale_source="$(jq -c "._source" <<<"${before}")"
    status="$(kubectl exec -n "${TEAM_NAMESPACE}" "${es_pod}" -- \
      curl --silent --max-time 5 -o /dev/null -w '%{http_code}' -X PUT \
      -H 'Content-Type: application/json' -d "${stale_source}" \
      "http://${release}-es-master:9200/${destination}/_doc/${case_data_id}?version=${stale_version}&version_type=external")"
    [[ "${status}" == "409" ]] || { echo "Stale replay was not rejected in ${destination}." >&2; exit 1; }
    after="$(kubectl exec -n "${TEAM_NAMESPACE}" "${es_pod}" -- \
      curl --fail --silent --max-time 5 "http://${release}-es-master:9200/${destination}/_doc/${case_data_id}")"
    jq -e --argjson before "${before}" '._version == $before._version and ._source == $before._source' \
      <<<"${after}" >/dev/null
  fi
done

echo "Manual requeue smoke passed for case ${LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE}."

evidence_dir="Logstash Manual Requeue Smoke"
mkdir -p "${evidence_dir}"
cat > "${evidence_dir}/evidence.md" <<EOF
# CCD-4262 manual-requeue smoke evidence

- UTC completion: $(date -u +%Y-%m-%dT%H:%M:%SZ)
- Jenkins branch: ${BRANCH_NAME}
- Preview namespace: ${TEAM_NAMESPACE}
- Case reference: ${LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE}
- Case data ID: ${case_data_id}
- Inserted queue ID: ${queue_id}
- Elasticsearch index: ${index}
- Mode: ${smoke_mode}
- Outage evidence: ${LOGSTASH_MANUAL_REQUEUE_OUTAGE_EVIDENCE:-not applicable}
- Cutover evidence: ${LOGSTASH_CUTOVER_EVIDENCE:-not applicable}
- Verified destinations: ${indices[*]}
- Result: queue row consumed; Elasticsearch version equals queue ID and exact supplementary data verified

Complete separately in the change ticket:

- Elasticsearch write-block start/end time and Logstash failure evidence
- DLQ document evidence
- Alert URL, receiving team, fired timestamp, and receipt confirmation
EOF
