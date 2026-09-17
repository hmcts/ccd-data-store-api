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

## Deferred design

Lease/claim processing is deferred. It must include a post-Elasticsearch ACK and
destination identity before it can be used; without an ACK successful rows are
reclaimed forever. A future design must also use `FOR UPDATE OF q SKIP LOCKED`
when joining the queue to `case_data`.

## Flyway compatibility

The earlier CCD-7841 claim-column migration is retained so environments that
already applied it continue to validate. CCD-4262 removes those unused columns
and index with a later, forward-only migration.
