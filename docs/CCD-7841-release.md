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

Flyway applies each migration transactionally. Repeated writes to a hot case
leave one outstanding queue row; the poll indexes its current state.

| Migration | Change | Operational effect |
| --- | --- | --- |
| `V20240617_4775` | Drops the old `trg_case_data_updated` marker trigger and installs the queue trigger. | Happens during Data Store migration, before the bigint migration; subsequent changes accumulate in the queue. |
| `V20260910_0000` | Adds the historical claim columns. | Retained for Flyway history in environments that already applied it. |
| `V20260917_0000` | Removes the unused claim columns. | Removes the former claim mechanism. |
| `V20260917_0001` | Widens the queue ID and sequence to `bigint` before coalescing. | Prevents integer exhaustion while allocating replacement backlog IDs. Safe to apply after the later bigint migration. |
| `V20260918_0000` | Rebuilds the backlog with one fresh row per case, adds unique `case_data_id`, and uses `ON CONFLICT DO NOTHING`. | Coalesces repeated writes; rewrites the queue in one transaction. |
| `V20260923_0000` | Widens both queue ID and sequence to `bigint`; restarts above the greatest of `10^10`, the sequence value and existing queue IDs; assigns fresh backlog IDs. | Protects the cutover from legacy ES versions. Sequence restart rolls back if the migration fails. |

`LogstashQueueMigrationCallback` makes the queue foreign-key checks immediate
only inside the September 18 migration transaction. This prevents pending
foreign-key events from blocking its `ADD CONSTRAINT` after a backlog rewrite,
without editing that migration's checksum or changing the schema's deferred
foreign-key default. Run migrations through the Data Store application so its
Flyway callback is registered; a standalone Flyway CLI run must register the
same callback.

## Deployment prerequisites

> [!WARNING]
> **Verify production ES versions before deployment.** Existing `_version` values must be below `10^10` in every destination case index and `global_search`. Record evidence; sampling alone is insufficient. Do not proceed with an unverified or higher baseline.

| Check | Required action / evidence |
| --- | --- |
| Production ES versions | Verify `_version` is below `10^10` across every destination case index and `global_search`. Record evidence before deployment. A sample does not prove the maximum; resolve an unverified or higher baseline before proceeding. |
| Queue size and deployment window | Record queue-row count; drain or reduce a large backlog. Measure the complete migration duration against a representative backlog and reserve a case-write maintenance window for column conversion and both queue rewrites. |
| Lock timeout | Coalescing and version migrations use a 15-second lock timeout for acquiring locks; it does not limit migration duration or time holding locks. Queue locks also block case writes through the trigger. Investigate contention and retry the deployment if reached. |
| Previously migrated environments | Confirm effective `spring.flyway.out-of-order=true` (the repository default) so `V20260917_0001` runs even where September 18 or later migrations have already applied. Verify its successful entry in Flyway history before resuming writes. Do not modify applied migration files. |
| Flux configuration | Deploy `cnp-flux-config` Option 2 queue polling, external queue-ID versioning and its operational guidance with this release. |
| Embedded chart consumers | Track `hmcts-charts` alignment and ownership in the related Jira ticket. Confirm active consumers and migration compatibility before changing these templates. |
| Scope | Leave standalone bulk-reindex pipelines unchanged. |
| Release evidence | Complete the application validation and Platform alert checks below before production rollout. |

## Deployment order

> [!WARNING]
> **Pause case writes and stop and drain old consumers before migrations.** Queue rewrites require a case-write maintenance window, not just search downtime. Start flux only after all Data Store migrations succeed. The old marker trigger is removed during migration; search stays stale until the new consumers start and drain the queue. Never overlap old and new writers.

| Step | Action | Check / expected behaviour |
| --- | --- | --- |
| 1 | Pause all case-writing API traffic and background jobs, let in-flight writes finish, then stop and drain all old Logstash consumers. | Confirm writes are quiescent; no overlap between internal-version and external-version writers. |
| 2 | Deploy Data Store and complete all migrations. | The marker trigger is replaced during migration; changes accumulate in the queue. Search is temporarily stale. |
| 3 | Verify the migrated queue. | Both column and sequence are `bigint`; backlog IDs exceed `10^10`. |
| 4 | Deploy/start the Option 2 flux pipelines immediately. | Start only after all migrations succeed; flux cannot run before its queue table exists. |
| 5 | Verify recovery of indexing. | Monitor queue drainage, output warnings and write latency. Verify a supplementary-data update in its case index and, where applicable, `global_search`. |
| 6 | Resume case-writing traffic and background jobs after the migration and indexing checks pass. | Monitor write latency and queue drainage. Keep writes paused if migration fails until database readiness is verified. |

## Validation and release evidence

| Validation | Required coverage / evidence |
| --- | --- |
| Application and configuration tests | Trigger/constraint coalescing, bounded polling, external queue-ID versioning, supplementary-data indexing and configuration contracts. |
| Migration regression tests | Legacy int4 conversion, backlog reversioning, higher sequence/explicit queue IDs and an empty queue; real Flyway upgrade and out-of-order history validation. |
| Elasticsearch regression tests | Internal-to-external version migration above the 32-bit limit and rejection of stale events. |
| F-106/S-609 and S-610 (`@F-7841`) | Rapid updates converge; a second supplementary-only update also replaces a value already confirmed searchable. |
| Preview cutover gate | Run the [cutover procedure](CCD-4262-logstash-queue-processing.md#cutover-end-to-end-release-gate): legacy documents and backlog through real migrations/Logstash to both indices, then stale replay rejection. |
| Jenkins outage/manual-requeue smoke | Elasticsearch recovery through targeted manual requeue. This does not prove alert delivery. |
| Platform alert verification | Before production rollout, test Elasticsearch output-failure and dead-letter-index alerts in preview. Platform owns rules and routing; record alert URL, recipient, firing time and receipt in CCD-4262. |

## Recovery and rollback

> [!WARNING]
> **Reverting flux alone does not restore indexing.** The old marker trigger has been removed. Stop consumers, preserve the queue and prefer fixing forward. Restoring marker processing requires database changes and reconciliation. Never reset the queue sequence backwards.

| Situation | Action |
| --- | --- |
| Elasticsearch or pod failure after queue deletion | Manually requeue affected cases using the bounded [recovery query](../src/main/resources/db/useful-queries/logstash_re_indexing_query.sql), narrowed by jurisdiction, case type, references or failure window. It writes only `case_data_logstash_queue`; never reset `case_data.marked_by_logstash`. |
| Flux deployment fails after migration | Preserve the queue; search stays stale until consumers resume. Prefer fixing forward. |
| Rollback required | Stop new consumers and preserve the queue. Reverting flux alone does not restore the marker trigger. Restoring marker processing requires restoring database behaviour, reconciling cutover changes and planning a fresh version baseline before retrying. Never reset the queue sequence backwards. |
| Conflict with a legacy ES version | Resolve the version baseline, then requeue affected cases. |

Run the recovery query's entire DO block with **autocommit enabled**, outside an
explicit transaction. It automatically traverses jurisdictions in batches of up
to 1,000 cases, committing each batch and tracking progress independently of queue
consumption. Interrupted runs retain committed batches and can be restarted as a
fresh pass. Completion confirms queueing only: verify Elasticsearch delivery and
check Logstash output failures/DLQ; resolve failures and requeue affected cases.

See [CCD-4262 Logstash queue processing](CCD-4262-logstash-queue-processing.md)
for detailed operational procedures and preview evidence requirements.

## Coalescing and conflict handling

| Topic | Decision / implication | Mitigation |
| --- | --- | --- |
| Coalescing | Retain the unique case constraint. A case write can wait on the uniqueness check until a poll transaction finishes, despite polling locking only queue rows. | Validate latency under representative concurrent load and monitor during rollout. The observed approximately 10 ms is not an upper bound. Batch deduplication without uniqueness is deferred. |
| Retry setting | Remove `retry_on_conflict => 0` and its misleading comment from data-store and both flux pipelines; it does not retry index actions. | Use output monitoring and the documented manual recovery procedure. |
| Expected 409 | An older event is rejected after a newer event succeeds. | No recovery needed for the superseded event. |
| Unsafe 409 | A legacy internal version blocks the latest queued update, which can then be lost. | Establish the baseline before deployment; monitor output warnings separately from the DLQ because 409s are normally logged and dropped. |

## Marker trigger and column removal timing

| Item | Timing / requirement |
| --- | --- |
| Old marker trigger | Dropped by `V20240617_4775__CCD-4775_modify_db_trigger_for_IU.sql` during Data Store migration, before the bigint migration. This is not a manual cleanup after flux deployment. |
| Replacement queue trigger | Installed by the same migration; captures subsequent case changes during the search-staleness window. Stop old consumers before migrations and start new consumers only after all migrations succeed. |
| `marked_by_logstash` column | Post-cutover follow-up: [PR #2452 — CCD-4262-4790_Drop_marked_by_logstash_field_case_data](https://github.com/hmcts/ccd-data-store-api/pull/2452). Deploy only after every active consumer is confirmed on Option 2 queue processing and indexing is verified; completing database migrations alone is insufficient. |
| Deferring marker-trigger removal | Requires a different, staged compatibility rollout; this is not the current migration order. |
