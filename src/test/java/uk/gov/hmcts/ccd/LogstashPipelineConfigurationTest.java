package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogstashPipelineConfigurationTest {

    private static final Path PREVIEW_VALUES =
        Path.of("charts/ccd-data-store-api/values.preview.template.yaml");

    @Test
    void previewLogstashPipelineShouldDeleteBoundedQueueBatchesUsingQueueIdsAsExternalVersions() throws IOException {
        String previewValues = Files.readString(PREVIEW_VALUES);

        assertAll(
            () -> assertTrue(
                previewValues.contains("document_id => \"%{id}\""),
                "Logstash output must keep stable document ids"
            ),
            () -> assertTrue(
                previewValues.contains("version => \"%{version}\""),
                "Preview Logstash output must use the queue row version as the external version"
            ),
            () -> assertTrue(
                previewValues.contains("version_type => \"external\""),
                "Preview Logstash output must use Elasticsearch external versioning"
            ),
            () -> assertTrue(
                previewValues.contains("RETURNING q.id AS version"),
                "Preview Logstash input must expose the monotonic queue row id as version"
            ),
            () -> assertTrue(
                previewValues.contains("ORDER BY q.id")
                    && previewValues.contains("FOR UPDATE SKIP LOCKED")
                    && previewValues.contains("LIMIT 1000"),
                "Preview Logstash input must delete a bounded, lock-safe queue batch"
            ),
            () -> assertTrue(
                previewValues.contains("DELETE FROM case_data_logstash_queue q"),
                "Queue rows must have a terminal state after they are read"
            ),
            () -> assertTrue(
                previewValues.contains("dead_letter_queue.enable: true")
                    && previewValues.contains("pipeline.id: index-dead-letter-to-es")
                    && previewValues.contains("index => \"ccd-logstash-dead-letter\""),
                "Preview Logstash must route non-retryable Elasticsearch failures to the dead-letter index"
            )
        );
    }
}
