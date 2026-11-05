# CCD-4790 marked_by_logstash one-off migration

This is a one-off, release-specific migration guide for removing `case_data.marked_by_logstash`. It is not part of routine database cleanup or regular operational maintenance.

## Related files

| File | Purpose |
|------|---------|
| `V20260702_0000__CCD-6936_skip_case_pointers_in_logstash_queue.sql` | Drops the legacy pointer constraint and updates the queue trigger to skip pointer rows. |
| `V20260703_0000__CCD-4790_Drop_marked_by_logstash_field_case_data.sql` | Drops the old trigger/function/index/constraint/column after the completion marker is present. |
| `../useful-queries/logstash_re_indexing_query.sql` | Queues cases into `case_data_logstash_queue`. |
| `../useful-queries/logstash_re_indexing_validation_query.sql` | Checks queue drain status, expected Elasticsearch counts, and sample document ids. |
| `../useful-queries/ccd_4790_marked_by_logstash_drop_ready_marker.sql` | Records the one-off completion marker after queue drain and Elasticsearch validation pass. |

## One-off release steps

1. Complete the queue-consumer cutover described in [CCD-7841 release notes](../../../../../docs/CCD-7841-release.md). Confirm every active Logstash consumer uses queue-based indexing and no active consumer reads or writes `marked_by_logstash`.
2. Pause case-writing traffic and background jobs, and allow in-flight writes to finish. Keep writes paused until migration and indexing checks complete.
3. Retain existing Elasticsearch indexes. Column removal does not require index deletion or a full rebuild. If recovery is needed, run `../useful-queries/logstash_re_indexing_query.sql` for the affected cases, following its prerequisites.
4. Allow queued events to finish indexing. Confirm the queue has drained and resolve any Elasticsearch output failures or dead-letter entries; queue deletion alone does not prove delivery.
5. Run `../useful-queries/logstash_re_indexing_validation_query.sql`, compare expected counts with Elasticsearch `_count` results, and verify sampled documents contain the expected current data.
6. Record the consumer-cutover and indexing evidence, then run `../useful-queries/ccd_4790_marked_by_logstash_drop_ready_marker.sql`.
7. Deploy the CCD-4790 column-drop migration only after the readiness criteria below pass. Confirm the backdated migration runs with Flyway out-of-order enabled on environments that have already applied later migrations.
8. Verify queue-based indexing still works after deployment, then resume normal writes.

## Readiness criteria

The database is ready for the CCD-4790 column drop only when:

1. All active consumers use queue-based indexing; legacy consumers are stopped and will not be restarted.
2. Case writes are paused, in-flight indexing is complete, and output failures/dead-letter entries have been resolved.
3. `remaining_logstash_queue_rows` is `0`.
4. Elasticsearch `_count` results match the expected counts from `logstash_re_indexing_validation_query.sql`.
5. Sampled `_doc` lookups return the expected current data.
6. The `CCD-4790-marked-by-logstash-drop-ready` marker exists after running `ccd_4790_marked_by_logstash_drop_ready_marker.sql`.

Elasticsearch validation examples:

```bash
curl -XPOST "$ES_URL/<index-name>/_refresh"
curl "$ES_URL/<index-name>/_count"
curl "$ES_URL/<index-name>/_doc/<document-id>"
```

## Guard behaviour

`V20260703_0000__CCD-4790_Drop_marked_by_logstash_field_case_data.sql` blocks populated databases unless the `CCD-4790-marked-by-logstash-drop-ready` marker exists.

The marker check and insertion execute atomically in one `DO` block. The marker records that the one-off readiness checks have been completed; it is an operator attestation, not an automatic consumer or Elasticsearch check. Reconfirm readiness before deployment if the environment changes after the marker is recorded. The database cannot independently prove Logstash has drained the queue or that Elasticsearch has fully caught up; that proof requires the combined readiness checks above.
