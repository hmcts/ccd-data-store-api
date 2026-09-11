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
    void previewLogstashPipelineShouldPreserveExternalVersioningAndSafeClaims() throws IOException {
        String previewValues = Files.readString(PREVIEW_VALUES);

        assertAll(
            () -> assertTrue(
                previewValues.contains("document_id => \"%{id}\""),
                "Logstash output must keep stable document ids"
            ),
            () -> assertTrue(
                previewValues.contains("version => \"%{version}\""),
                "Preview Logstash output must use case_data.version as the external version"
            ),
            () -> assertTrue(
                previewValues.contains("version_type => \"external\""),
                "Preview Logstash output must use Elasticsearch external versioning"
            ),
            () -> assertTrue(
                !previewValues.contains("version => \"%{[@metadata][version]}\""),
                "Preview Logstash output must not use queue metadata as an external version"
            ),
            () -> assertTrue(
                previewValues.contains("FOR UPDATE SKIP LOCKED"),
                "Logstash input must claim queue rows without competing with another agent"
            ),
            () -> assertTrue(
                previewValues.contains("LOGSTASH_QUEUE_CLAIM_TIMEOUT")
                    && previewValues.contains("CAST(:claim_timeout AS interval)"),
                "Queue claim timeout must be configurable"
            ),
            () -> assertTrue(
                previewValues.contains("claim_token"),
                "Logstash input must retain the claim token for safe acknowledgement"
            ),
            () -> assertTrue(
                !previewValues.contains("DELETE FROM case_data_logstash_queue USING case_data"),
                "Logstash input must not delete queue rows before Elasticsearch succeeds"
            ),
            () -> assertTrue(
                !previewValues.contains("statement => [\"DELETE FROM case_data_logstash_queue"),
                "Logstash must not unconditionally delete queue rows through a second output"
            )
        );
    }
}
