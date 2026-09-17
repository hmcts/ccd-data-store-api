package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogstashPipelineConfigurationTest {

    private static final Path PREVIEW_VALUES =
        Path.of("charts/ccd-data-store-api/values.preview.template.yaml");

    @Test
    void previewLogstashPipelineShouldDeleteBoundedQueueBatchesUsingQueueIdsAsExternalVersions() throws IOException {
        String previewValues = Files.readString(PREVIEW_VALUES);
        String input = pipelineBlock(previewValues, "01_input.conf", "02_filter.conf");
        String output = pipelineBlock(previewValues, "03_output.conf", "dead_letter_indexing_pipeline.conf");

        assertAll(
            () -> assertTrue(
                output.contains("document_id => \"%{id}\""),
                "Logstash output must keep stable document ids"
            ),
            () -> assertTrue(
                output.contains("version => \"%{version}\""),
                "Preview Logstash output must use the queue row version as the external version"
            ),
            () -> assertTrue(
                output.contains("version_type => \"external\""),
                "Preview Logstash output must use Elasticsearch external versioning"
            ),
            () -> assertTrue(
                input.contains("RETURNING q.id AS version"),
                "Preview Logstash input must expose the monotonic queue row id as version"
            ),
            () -> assertTrue(
                input.contains("ORDER BY q.id")
                    && input.contains("FOR UPDATE SKIP LOCKED")
                    && input.contains("LIMIT 1000"),
                "Preview Logstash input must delete a bounded, lock-safe queue batch"
            ),
            () -> assertTrue(
                input.contains("DELETE FROM case_data_logstash_queue q"),
                "Queue rows must have a terminal state after they are read"
            ),
            () -> assertFalse(input.contains("claim_token") || input.contains("claimed_at"),
                "Preview Logstash input must not retain the removed lease/claim path"),
            () -> assertTrue(
                previewValues.contains("dead_letter_queue.enable: true")
                    && previewValues.contains("pipeline.id: index-dead-letter-to-es")
                    && previewValues.contains("index => \"ccd-logstash-dead-letter\""),
                "Preview Logstash must route non-retryable Elasticsearch failures to the dead-letter index"
            )
        );
    }

    private String pipelineBlock(String values, String start, String end) {
        int startIndex = values.indexOf(start);
        int endIndex = values.indexOf(end, startIndex);
        return values.substring(startIndex, endIndex);
    }
}
