# CCD-7841 release notes: Logstash queue processing

## Scope and decision

This release adopts CCD-4262 Option 2 for CCD Logstash indexing: a bounded,
plain-delete queue poll. Logstash locks and deletes at most 1000 queue rows,
then indexes the current case document. The queue ID is the Elasticsearch
external version.

Delivery is deliberately at-most-once. Elasticsearch or pod failure after the
queue delete is recovered by manually re-queuing only the affected cases; it is
not an automatic lease/retry design.

## Related tickets

| Ticket | Release relevance |
| --- | --- |
| CCD-7841 | Preview outage validation and the targeted manual-requeue smoke. |
| CCD-4262 | Removes `case_data` Logstash-marker write contention; adds bounded queue polling, recovery guidance, DLQ evidence, and one-outstanding-row coalescing. |
| CCD-4769 | Original `case_data_logstash_queue` schema on which this release depends. |
| CCD-6395 | Included in the branch ancestry; confirm its separate business scope before adding a release claim. No queue-processing behaviour is attributed to it here. |
| CCD-4311 | Separate preview-only token-claim-validation exception; not part of Logstash release behaviour. See [CCD-4311 token-claim validation](CCD-4311-token-claim-validation.md). |

## Database and queue behaviour

Flyway applies the changes atomically:

1. `V20260910_0000__CCD-7841_claim_logstash_queue_rows.sql` is retained only
   for Flyway history in environments that already applied it; its claim columns
   are removed by `V20260917_0000__CCD-4262_drop_logstash_queue_claim_columns.sql`;
2. `V20260918_0000__CCD-4262_coalesce_logstash_queue_rows.sql` converts any
   existing queue backlog into one new row per case, adds the unique
   `case_data_id` constraint, and updates the trigger to use `ON CONFLICT DO
   NOTHING`; and
3. repeated writes to a hot case therefore leave one outstanding queue row,
   while the poll indexes its current state.

`marked_by_logstash` is not read or written by this queue path. Its schema
removal remains a separate CCD-4790 rollout after every active consumer has
been confirmed on Option 2.

## Deployment prerequisites

- Deploy the Data Store migration and Option 2 Logstash configuration together.
- Before production deployment, record the queue-row count and drain or reduce a
  large backlog. The coalescing migration rewrites the queue in one transaction;
  run it in a controlled, low-write deployment window and retry the deployment if
  its 15-second lock timeout is reached.
- `cnp-flux-config` already uses the Option 2 queue poll and external queue-ID
  versioning; its operational guidance must be deployed with this release.
- Embedded `hmcts-charts` Logstash templates are a follow-up: confirm active
  consumers and Data Store migration compatibility before changing them. Track
  their alignment and ownership in the related Jira ticket.
- Do not change standalone bulk-reindex pipelines as part of this release.

## Validation and release evidence

Application validation covers the trigger/constraint coalescing behaviour,
bounded poll, external queue-ID versioning, supplementary-data indexing, and
configuration contracts. F-106/S-609 (tagged `@F-7841`) verifies that two
consecutive supplementary-data updates are indexed as their final value. The
targeted Jenkins outage/manual-requeue smoke covers Elasticsearch recovery.

Platform monitoring owns the alert rules and notification route. Before
production rollout, Platform must test an Elasticsearch output-failure alert
and a dead-letter-index alert in preview, then record the alert URL, recipient,
firing time, and receipt in CCD-4262. The Jenkins smoke cannot prove alert
delivery.

## Recovery

Use the bounded insert-based recovery query in
[logstash_re_indexing_query.sql](../src/main/resources/db/useful-queries/logstash_re_indexing_query.sql),
narrowed to the known jurisdiction, case type, references, or failure window.
It only writes `case_data_logstash_queue`; never reset
`case_data.marked_by_logstash`.

For the detailed operational procedure and preview evidence requirements, see
[CCD-4262 Logstash queue processing](CCD-4262-logstash-queue-processing.md).
