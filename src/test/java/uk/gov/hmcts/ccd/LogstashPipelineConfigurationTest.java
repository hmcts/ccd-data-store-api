package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogstashPipelineConfigurationTest {

    private static final Path PREVIEW_VALUES =
        Path.of("charts/ccd-data-store-api/values.preview.template.yaml");
    private static final Path COALESCING_MIGRATION =
        Path.of("src/main/resources/db/migration/V20260918_0000__CCD-4262_coalesce_logstash_queue_rows.sql");
    private static final Pattern JDBC_STATEMENT = Pattern.compile(
        "statement => \\\"(.*?)\\\"\\s+clean_run", Pattern.DOTALL);
    private static final String EXPECTED_POLL_STATEMENT = """
        WITH candidates AS (
        SELECT q.id
        FROM case_data_logstash_queue q
        ORDER BY q.id
        FOR UPDATE SKIP LOCKED
        LIMIT 1000
        )
        DELETE FROM case_data_logstash_queue q
        USING candidates c, case_data cd
        WHERE q.id = c.id
        AND q.case_data_id = cd.id
        RETURNING q.id AS version, cd.id, created_date, last_modified, jurisdiction, case_type_id, state,
        last_state_modified_date, data::TEXT AS json_data, data_classification::TEXT AS json_data_classification,
        reference, security_classification, supplementary_data::TEXT AS json_supplementary_data
        """;

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
            () -> assertThat(normaliseWhitespace(jdbcStatement(input)))
                .as("Preview Logstash must use the complete bounded plain-delete queue poll contract")
                .isEqualTo(normaliseWhitespace(EXPECTED_POLL_STATEMENT)),
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

    @Test
    void coalescingMigrationShouldDiscardLegacyPointerRowsAndEnforceOneOutstandingRow() throws IOException {
        String migration = Files.readString(COALESCING_MIGRATION);

        assertAll(
            () -> assertTrue(
                migration.contains("JOIN public.case_data cd")
                    && migration.contains("WHERE NOT (cd.data = '{}'::jsonb AND cd.state = '')"),
                "Backlog coalescing must not re-queue legacy case pointers"
            ),
            () -> assertTrue(
                migration.contains("UNIQUE (case_data_id)"),
                "The queue must permit only one outstanding row per case"
            ),
            () -> assertTrue(
                migration.contains("ON CONFLICT (case_data_id) DO NOTHING"),
                "The trigger must coalesce concurrent updates"
            )
        );
    }

    private String pipelineBlock(String values, String start, String end) {
        int startIndex = values.indexOf(start);
        int endIndex = values.indexOf(end, startIndex);
        return values.substring(startIndex, endIndex);
    }

    private String jdbcStatement(String input) {
        Matcher matcher = JDBC_STATEMENT.matcher(input);
        assertThat(matcher.find()).as("JDBC input statement").isTrue();
        return matcher.group(1);
    }

    private String normaliseWhitespace(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }
}
