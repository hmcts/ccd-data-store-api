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
appropriate. Each execution inserts at most 1000 queue rows; repeat it until no
rows are inserted. The recovery query never updates `case_data`.

Operational monitoring must alert on Elasticsearch output failures, including
non-retryable failures and DLQ routing, so the affected window is known promptly.

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
test when a case reference is supplied. It refuses any non-PR namespace. Set
these build variables from the outage scenario's recorded case:

```
LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE=<numeric case reference>
LOGSTASH_MANUAL_REQUEUE_CASE_TYPE=AAT_PRIVATE
LOGSTASH_MANUAL_REQUEUE_EXPECTED_SUPPLEMENTARY_DATA={"orgs_assigned_users":{"OrgA":22,"OrgB":1}}
```

Without `LOGSTASH_MANUAL_REQUEUE_CASE_REFERENCE`, normal preview builds log a
skip. Supply the values only for the targeted release-gate run.

It re-queues just that case in the preview PostgreSQL pod, waits for the queue
row to be consumed, and verifies its Elasticsearch document contains the
expected supplementary data. Do not supply a case reference in normal PR builds;
this is a targeted release-gate smoke.

The required preparation is:

1. Create an `AAT_PRIVATE` case in the isolated preview deployment and add
   supplementary data; record the case ID and the UTC start/end time.
2. Block writes to that preview Elasticsearch index, wait for Logstash to poll
   the queue, then restore writes. Confirm the failure is visible in Logstash and
   the DLQ/dead-letter index where the failure is non-retryable.
3. Enable the Jenkins hook with the recorded values above. It re-queues only that
   case, waits for the queue row to be consumed, and verifies its Elasticsearch
   document contains the expected supplementary data.
4. Confirm the alert fired and was received by its owner, then attach the Jenkins
   result and alert evidence to the ticket.

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

## Deferred design

Lease/claim processing is deferred. It must include a post-Elasticsearch ACK and
destination identity before it can be used; without an ACK successful rows are
reclaimed forever. A future design must also use `FOR UPDATE OF q SKIP LOCKED`
when joining the queue to `case_data`.

## Flyway compatibility

The earlier CCD-7841 claim-column migration is retained so environments that
already applied it continue to validate. CCD-4262 removes those unused columns
and index with a later, forward-only migration.
