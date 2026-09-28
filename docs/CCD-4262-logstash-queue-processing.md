# CCD-4262: Logstash queue processing

## Decision

Use a bounded plain-delete queue poll. Logstash selects up to 1000 queue rows in
ID order, locks only those queue rows, deletes them, and sends the current case
document to Elasticsearch. The deleted queue ID is the Elasticsearch external
version, so a later queue entry always supersedes an earlier one. A unique queue
constraint and the trigger coalesce repeated writes into one outstanding row per
case; the poll therefore indexes the current state once for a hot case.

This removes the `case_data.marked_by_logstash` write contention addressed by
CCD-4262. Each selected queue row has a terminal database state, but Elasticsearch
delivery is not acknowledged.

## Failure recovery

This is at-most-once delivery: an Elasticsearch or pod failure after polling may
require manual recovery. Re-queue the affected cases using
`src/main/resources/db/useful-queries/logstash_re_indexing_query.sql`, narrowed
to the known jurisdiction, case type, references, or failure time window where
appropriate. Run the entire DO block with autocommit enabled, outside an explicit
transaction. It traverses cases in primary-key order in batches of at most 1000, committing
each batch and advancing by case ID independently of queue consumption. The pass
is bounded by the maximum case ID at its start; normal triggers queue new writes.
This avoids a full jurisdiction count/sort and repeated scans per jurisdiction;
only one small checkpoint row is stored per recovery, not a copy of all case IDs.
Set `recovery_name` in the script before running. Progress is stored in the persistent
`public.logstash_reindex_progress` table and committed atomically with each batch.
After interruption, rerun with the same name and unchanged filters to resume after
the last committed batch, even from a new connection. Use a new name for a fresh
recovery, changed filters, or recreated target indexes. Completed names do no work.
The first execution requires permission to create the checkpoint table; subsequent
runs require SELECT, INSERT and UPDATE on it. Retain checkpoints while recovery
may need resuming; deleting one makes that name start from the beginning. Never
delete checkpoints while their recovery is running. To remove a verified completed
run, use `DELETE FROM public.logstash_reindex_progress WHERE run_name = '<name>' AND completed;`.
Checkpoints record queueing progress, not Elasticsearch acknowledgements: to retry
failed delivery from earlier batches, use a new recovery name with appropriate filters.
The recovery query never updates `case_data`. Completion confirms queueing only:
check output failures/DLQ and verify Elasticsearch delivery before closing recovery.

Operational monitoring must alert on Elasticsearch output failures, including
non-retryable failures and DLQ routing, so the affected window is known promptly.

## Batch-write contract and required validation

The JDBC poll is a database batch, not an Elasticsearch bulk-size setting. It
must select, lock, delete, and return at most 1000 queue rows, using `q.id` as
the external Elasticsearch version. The 1000-row limit bounds the work and the
potentially affected rows per at-most-once poll failure; a prolonged outage can
require recovery of multiple batches. Logstash's pipeline batch setting controls
Elasticsearch request size independently and is a throughput tuning decision,
not a data-correctness requirement.

The Option 2 query contract is the source of truth; every deployment's embedded
statement must conform to it. The repository's preview configuration test
compares its complete query and projection with that contract; repository tests
exercise the same poll semantics against PostgreSQL, and Elasticsearch tests
prove stale external versions are rejected. Any poll-query change must update
and validate all three configuration sources:

- `ccd-data-store-api` preview Helm values and its configuration test;
- `cnp-flux-config` Data Store Logstash pipelines and queue-contract test; and
- `ccd-docker` Logstash pipeline and queue-contract test.

Required evidence for a release is a passing configuration contract, the focused
PostgreSQL and Elasticsearch tests, F-106/S-609 in preview, and the operational
outage/DLQ/manual-requeue evidence below. A bulk-size or worker-count change
additionally requires a representative backlog-drain measurement before rollout.

## Release gate: operational alert and manual-requeue smoke test

The Logstash pipeline enables its dead-letter queue and indexes non-retryable
entries into `ccd-logstash-dead-letter`. Before production rollout, the platform
monitoring owner must create and test an alert for new documents in that index
and for Logstash Elasticsearch output failures. Record the alert URL, receiving
team, and test timestamp in the change ticket; this repository cannot create an
environment-level alert. Production rollout must not proceed until that evidence
is recorded.

### Platform-operated alert test (outside application test scope)

This mandatory release test is owned by Platform monitoring, not BEFTA or the
application test suite: alert rules and notification routes are managed outside
this repository. In an isolated preview environment, Platform must:

1. block Elasticsearch writes and confirm the Logstash output-failure alert;
2. create a controlled non-retryable indexing failure and confirm the
   dead-letter-index alert; and
3. record the alert URL, recipient, firing time, and receipt in CCD-4262.

The Jenkins smoke below verifies manual requeue and successful index recovery;
it does not prove alert delivery.

Run this smoke test in an isolated preview deployment after the alert is active.
The Jenkins hook is enabled for preview and runs after a successful preview smoke
test when `LOGSTASH_MANUAL_REQUEUE_SMOKE_ENABLED=true`. It requires a PR branch
and a `pr-*` namespace, or namespace `ccd` on a `cft-preview-*-aks` Kubernetes
context. Resource selection remains scoped to that PR's release. Set
these build variables from the outage scenario's recorded case:

```
LOGSTASH_MANUAL_REQUEUE_SMOKE_ENABLED=true
LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE=<numeric case reference>
LOGSTASH_MANUAL_REQUEUE_CASE_TYPE=AAT_PRIVATE
LOGSTASH_MANUAL_REQUEUE_EXPECTED_SUPPLEMENTARY_DATA={"orgs_assigned_users":{"OrgA":22,"OrgB":1}}
LOGSTASH_MANUAL_REQUEUE_OUTAGE_EVIDENCE=<link to recorded outage and Logstash failure evidence>
```

Normal preview builds leave the hook disabled. When enabled, missing case
reference, exact expected data or outage evidence fails the build. Supply these
values only for the targeted release-gate run.

It re-queues just that case in the preview PostgreSQL pod, waits for the queue
row to be consumed, then polls Elasticsearch until the document version equals
the inserted queue ID and its supplementary data exactly matches the expected JSON.
For cases with `SearchCriteria`, it also verifies `global_search`; supply a known
`HMCTSServiceId` in the expected data for that destination. Do not supply a case reference in normal PR builds;
this is a targeted release-gate smoke.

The required preview procedure is:

1. Confirm Platform monitoring has alerts for Logstash Elasticsearch-output
   failures and for new `ccd-logstash-dead-letter` documents. Use an isolated PR
   preview only.
2. Create an `AAT_PRIVATE` case and add known supplementary data. Record the
   numeric reference, exact expected JSON, and UTC start time.
3. Block writes to that preview index only:

   ```bash
   curl -X PUT "http://<preview-es>:9200/aat_private_cases*/_settings" \
     -H 'Content-Type: application/json' \
     -d '{"index.blocks.write": true}'
   ```

4. Submit another supplementary-data update and wait for Logstash to poll.
   Capture the output failure and confirm its alert fires and is received.
5. Using a Platform-approved controlled non-retryable failure, confirm a
   document reaches `ccd-logstash-dead-letter` and its alert fires and is
   received.
6. Restore writes immediately after collecting the failure evidence:

   ```bash
   curl -X PUT "http://<preview-es>:9200/aat_private_cases*/_settings" \
     -H 'Content-Type: application/json' \
     -d '{"index.blocks.write": false}'
   ```

7. Enable the Jenkins hook with the recorded variables above. It re-queues only
   that case, waits for the queue row to be consumed, and verifies its
   Elasticsearch document contains the expected supplementary data.
8. Confirm both alerts fired and were received, then attach the Jenkins result
   and alert evidence to CCD-4262.

Jenkins archives `Logstash Manual Requeue Smoke/evidence.md`, which records the
case, queue ID, Elasticsearch verification, and UTC completion time. Add the
following platform-owned evidence to the same change ticket:

- Elasticsearch write-block start/end time and the Logstash failure;
- the DLQ/dead-letter document; and
- alert URL, receiving team, fired timestamp, and receipt confirmation.

For an operator-run fallback, re-queue only the recorded case in the Data Store
PostgreSQL pod:

```sql
INSERT INTO case_data_logstash_queue (case_data_id)
SELECT id FROM case_data cd
WHERE cd.reference = :case_reference
  AND NOT EXISTS (
    SELECT 1 FROM case_data_logstash_queue q WHERE q.case_data_id = cd.id
  )
ON CONFLICT (case_data_id) DO NOTHING;
```

This is a manually initiated, Jenkins-automated preview smoke: Option 2
deliberately has no automatic retry after the queue row has been deleted.

## Cutover end-to-end release gate

### Local smoke-script regression tests

`./gradlew test` also runs the mocked smoke-script tests via
`logstashSmokeTest`. Run only these tests with `./gradlew logstashSmokeTest`.
Python 3, Bash and jq are required; missing jq fails rather than skipping coverage.
JUnit XML is written to `build/test-results/logstashSmokeTest/TEST-logstash-smoke.xml`
and the HTML report to `build/reports/tests/logstashSmokeTest/index.html`.
Jenkins publishes the XML results and the **Logstash Smoke Script Tests** HTML
report, including failures. These tests do not replace the live preview gate below.

### Live preview procedure

Run separately from recovery in an isolated preview using the actual deployed
Logstash configuration. Keep case writes paused throughout this check.

1. Before upgrading, create a case with `SearchCriteria`, a known
   `HMCTSServiceId` and known supplementary data. Index its old state using the
   legacy consumer. Record its ID and internal ES versions in both its case
   index and `global_search`.
2. Stop/drain consumers, change the fixture case's supplementary data, and
   record that the changed case is in the queue while both ES documents remain
   at their old values. Pause case writes and run Data Store migrations.
3. Before restarting consumers, record this case's migrated queue ID (above
   `10^10`). Restart the real Logstash consumers without further fixture updates.
4. Enable the Jenkins hook with the case reference/type and exact expected
   supplementary JSON above, plus:

   ```text
   LOGSTASH_SMOKE_MODE=cutover
   LOGSTASH_CUTOVER_QUEUE_ID=<recorded migrated backlog ID>
   LOGSTASH_CUTOVER_EVIDENCE=<link to recorded legacy versions and backlog evidence>
   ```

   Cutover mode does not requeue the case. It checks successful Flyway history,
   queue drainage, and the exact migrated version and expected data in both
   destinations. It then submits an older external version to each destination,
   requires HTTP 409 and verifies the stored version and source are unchanged.
   It fails if the case lacks `SearchCriteria` or expected `HMCTSServiceId`.
5. Archive the Jenkins evidence with the legacy/backlog evidence before
   resuming normal writes. Run recovery mode separately with outage evidence.

`S-609` retains rapid-update coverage. `S-610` first waits for the initial
supplementary value to be searchable, then changes it and waits for the final
value, preventing both writes from being hidden by a single coalesced event.
Both searches are restricted to the newly created case reference.

## Deferred design

Lease/claim processing is deferred. It must include a post-Elasticsearch ACK and
destination identity before it can be used; without an ACK successful rows are
reclaimed forever. A future design must also use `FOR UPDATE OF q SKIP LOCKED`
when joining the queue to `case_data`.

## Flyway compatibility

The earlier CCD-7841 claim-column migration is retained so environments that
already applied it continue to validate. CCD-4262 removes those unused columns
and index with a later, forward-only migration.

## Production cutover and coalescing tradeoff

Follow the ordered deployment and rollback procedure in
[CCD-7841 release notes](CCD-7841-release.md#deployment-prerequisites).
The bigint migration raises the sequence above 10^10 (or any higher existing
sequence/queue ID) and gives the existing backlog fresh IDs. Verify the legacy
ES version bound for every destination, including `global_search`, before
switching consumers. Do not run old and new writers concurrently.

Pause case-writing traffic and background jobs for the migration window and
allow in-flight writes to finish. Queue locks block case writes through the
trigger. The 15-second lock timeout limits lock acquisition, not lock holding
time; measure the full migration against a representative backlog before rollout.
The earlier `V20260917_0001` migration widens IDs before coalescing can exhaust
the integer sequence. For previously migrated environments, verify the effective
Flyway out-of-order setting and successful migration history as described in
the release notes. Resume writes after migration and indexing checks pass.

The retained unique constraint means a case write can wait for a poll holding
its queue row. This is indirect contention despite the absence of case-row
locks in the poll. Measure write latency under load; the observed 10 ms is not
a guaranteed bound. Removing uniqueness and deduplicating batches is deferred.

Monitor Elasticsearch output warnings separately from the dead-letter index:
409s are normally dropped, not sent to the DLQ. An older event rejected after a
newer successful event is harmless; a legacy-version conflict requires baseline
correction and manual requeue. The at-most-once delivery limitation remains.

### Marker trigger removal timing

`V20240617_4775__CCD-4775_modify_db_trigger_for_IU.sql` drops the old
`trg_case_data_updated` marker trigger and installs the queue trigger during
Data Store migration. This happens before the bigint migration, not as a manual
cleanup after flux deployment. Stop old consumers before running migrations;
start the new consumers only after all migrations succeed. The replacement queue
trigger captures subsequent case changes during the search-staleness window.
Removing the `marked_by_logstash` column is a separate CCD-4790 change after
all consumers have migrated. Deferring the trigger removal itself would require
a different, staged compatibility rollout; it is not the current migration order.
