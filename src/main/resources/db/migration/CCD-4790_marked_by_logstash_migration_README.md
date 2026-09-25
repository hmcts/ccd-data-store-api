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

1. Complete the agreed data cleanup/migration activity.
2. Delete all Elasticsearch indexes.
3. Trigger ES re-indexing via ccd-admin-web to create the static index placeholders.
4. Run `../useful-queries/logstash_re_indexing_query.sql`.
5. Confirm `case_data_logstash_queue` has drained.
6. Run `../useful-queries/logstash_re_indexing_validation_query.sql`.
7. Compare expected counts with Elasticsearch `_count` results.
8. Verify sampled document ids with Elasticsearch `_doc` lookups.
9. Run `../useful-queries/ccd_4790_marked_by_logstash_drop_ready_marker.sql`.
10. Deploy the CCD-4790 column-drop migration only after the readiness criteria below pass.

## Readiness criteria

The database is ready for the CCD-4790 column drop only when:

1. `remaining_logstash_queue_rows` is `0`.
2. Elasticsearch `_count` results match the expected counts from `logstash_re_indexing_validation_query.sql`.
3. Sampled `_doc` lookups return the expected documents.
4. The `CCD-4790-marked-by-logstash-drop-ready` marker exists after running `ccd_4790_marked_by_logstash_drop_ready_marker.sql`.

Elasticsearch validation examples:

```bash
curl -XPOST "$ES_URL/<index-name>/_refresh"
curl "$ES_URL/<index-name>/_count"
curl "$ES_URL/<index-name>/_doc/<document-id>"
```

## Guard behaviour

`V20260703_0000__CCD-4790_Drop_marked_by_logstash_field_case_data.sql` blocks populated databases unless the `CCD-4790-marked-by-logstash-drop-ready` marker exists.

The marker records that the one-off readiness checks have been completed. The database cannot independently prove Logstash has drained the queue or that Elasticsearch has fully caught up; that proof requires the combined readiness checks above.
