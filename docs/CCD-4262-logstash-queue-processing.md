# CCD-4262: Logstash queue processing

## Decision

Use a bounded plain-delete queue poll. Logstash selects up to 1000 queue rows in
ID order, locks only those queue rows, deletes them, and sends the current case
document to Elasticsearch. The deleted queue ID is the Elasticsearch external
version, so a later queue entry always supersedes an earlier one.

This removes the `case_data.marked_by_logstash` write contention addressed by
CCD-4262 and gives each successful poll a terminal queue state.

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
environment-level alert.

Run this smoke test in an isolated preview deployment after the alert is active:

1. Create an AAT private case and add supplementary data; record the case ID and
   the UTC start/end time.
2. Block writes to the AAT private Elasticsearch index, wait for Logstash to poll
   the queue, then restore writes. Confirm the failure is visible in Logstash and
   the DLQ/dead-letter index where the failure is non-retryable.
3. In the Data Store PostgreSQL pod, re-queue only that case, substituting the
   recorded case ID:

   ```sql
   INSERT INTO case_data_logstash_queue (case_data_id)
   SELECT id FROM case_data cd
   WHERE cd.reference = :case_reference
     AND NOT EXISTS (
       SELECT 1 FROM case_data_logstash_queue q WHERE q.case_data_id = cd.id
     );
   ```

4. Confirm the queue row is consumed and `/searchCases` returns the case with the
   expected supplementary data. Confirm the alert fired and was received by its
   owner, then attach the evidence to the ticket.

This is a manual environment smoke test by design: Option 2 deliberately has no
automatic retry after the queue row has been deleted.

## Deferred design

Lease/claim processing is deferred. It must include a post-Elasticsearch ACK and
destination identity before it can be used; without an ACK successful rows are
reclaimed forever. A future design must also use `FOR UPDATE OF q SKIP LOCKED`
when joining the queue to `case_data`.

## Flyway compatibility

The earlier CCD-7841 claim-column migration is retained so environments that
already applied it continue to validate. CCD-4262 removes those unused columns
and index with a later, forward-only migration.
