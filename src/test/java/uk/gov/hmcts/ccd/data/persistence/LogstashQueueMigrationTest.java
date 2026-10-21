package uk.gov.hmcts.ccd.data.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LogstashQueueMigrationTest {
    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");
    private static final String WIDENING = "V20260917_0001__CCD-4262_widen_logstash_queue_id.sql";
    private static final String COALESCING = "V20260918_0000__CCD-4262_coalesce_logstash_queue_rows.sql";
    private static final String VERSIONING = "V20260923_0000__CCD-4262_raise_logstash_queue_version.sql";

    @TempDir
    Path migrationDirectory;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldUpgradeLegacyAndPreviouslyMigratedDatabases(boolean alreadyMigrated) throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")) {
            postgres.start();
            final JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
            // Minimal legacy case schema; the queue and upgrade SQL are production migrations.
            Files.writeString(migrationDirectory.resolve("V1__case_data.sql"),
                "CREATE TABLE public.case_data (id bigint PRIMARY KEY, data jsonb, state text);");
            String queueCreation = "V20240130_4769__CCD-4769_create_case_data_logstash_queue.sql";
            Files.copy(MIGRATIONS.resolve(queueCreation), migrationDirectory.resolve(queueCreation));
            Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("filesystem:" + migrationDirectory.toAbsolutePath())
                .outOfOrder(true)
                .callbacks(new LogstashQueueMigrationCallback())
                .load();
            flyway.migrate();
            jdbc.execute("INSERT INTO case_data VALUES (1, '{\"value\":1}', 'Created'), "
                + "(2, '{\"value\":2}', 'Created')");
            if (!alreadyMigrated) {
                jdbc.execute("ALTER SEQUENCE case_data_logstash_queue_id_seq RESTART WITH 2147483646");
            }
            jdbc.execute("INSERT INTO case_data_logstash_queue (case_data_id) VALUES (1), (2)");
            Files.copy(MIGRATIONS.resolve(COALESCING), migrationDirectory.resolve(COALESCING));
            Files.copy(MIGRATIONS.resolve(VERSIONING), migrationDirectory.resolve(VERSIONING));
            if (alreadyMigrated) {
                flyway.migrate();
            }
            Files.copy(MIGRATIONS.resolve(WIDENING), migrationDirectory.resolve(WIDENING));
            flyway.migrate();
            flyway.validate();

            assertThat(jdbc.queryForObject("SELECT condeferred FROM pg_constraint "
                + "WHERE conname = 'case_data_logstash_queue_case_data_id_fk'", Boolean.class)).isTrue();

            assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history "
                + "WHERE version = '20260917.0001'", Boolean.class)).isTrue();
            assertThat(jdbc.queryForList("SELECT case_data_id FROM case_data_logstash_queue", Long.class))
                .containsExactlyInAnyOrder(1L, 2L);
            assertThat(jdbc.queryForList("SELECT id FROM case_data_logstash_queue", Long.class))
                .allSatisfy(id -> assertThat(id).isGreaterThan(10000000000L));
            assertThat(jdbc.queryForObject("SELECT nextval('case_data_logstash_queue_id_seq')", Long.class))
                .isGreaterThan(jdbc.queryForObject("SELECT max(id) FROM case_data_logstash_queue", Long.class));
        }
    }
}
