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
    void previewLogstashPipelineShouldUseElasticsearchManagedDocumentVersion() throws IOException {
        String previewValues = Files.readString(PREVIEW_VALUES);

        assertAll(
            () -> assertTrue(
                previewValues.contains("document_id => \"%{id}\""),
                "Logstash output must keep stable document ids"
            ),
            () -> assertTrue(
                !previewValues.contains("version_type => \"external\""),
                "Preview Logstash output must let Elasticsearch manage document versions"
            ),
            () -> assertTrue(
                !previewValues.contains("version => \"%{version}\""),
                "Preview Logstash output must not use case_data.version as an external version"
            ),
            () -> assertTrue(
                !previewValues.contains("version => \"%{[@metadata][version]}\""),
                "Preview Logstash output must not use queue metadata as an external version"
            )
        );
    }
}
