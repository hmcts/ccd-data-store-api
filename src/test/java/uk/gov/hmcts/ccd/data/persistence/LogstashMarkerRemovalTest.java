package uk.gov.hmcts.ccd.data.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogstashMarkerRemovalTest {
    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");
    private static final String DROP = "V20260703_0000__CCD-4790_Drop_marked_by_logstash_field_case_data.sql";
    private static final Path MARKER = Path.of("src/main/resources/db/useful-queries/"
        + "ccd_4790_marked_by_logstash_drop_ready_marker.sql");
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");
    private JdbcTemplate jdbc;
    private Flyway flyway;

    @TempDir
    Path migrationDirectory;

    @BeforeAll
    static void startPostgres() {
        POSTGRES.start();
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    @BeforeEach
    void prepareSchema() throws Exception {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        jdbc.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public");
        jdbc.execute("""
            CREATE TABLE case_data (
                id bigint PRIMARY KEY, data jsonb, state text, data_classification jsonb,
                last_modified timestamp, last_state_modified_date timestamp,
                security_classification text, supplementary_data jsonb,
                case_type_id text DEFAULT 'TestCase', reference bigint, version integer DEFAULT 1,
                marked_by_logstash boolean DEFAULT false
            );
            CREATE INDEX idx_case_data_marked_by_logstash ON case_data (marked_by_logstash);
            CREATE FUNCTION set_case_data_marked_by_logstash() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN NEW.marked_by_logstash := false; RETURN NEW; END $$;
            CREATE TRIGGER trg_case_data_updated BEFORE INSERT OR UPDATE ON case_data
            FOR EACH ROW EXECUTE FUNCTION set_case_data_marked_by_logstash();
            """);
        for (String migration : List.of(
            "V20240130_4769__CCD-4769_create_case_data_logstash_queue.sql",
            "V20240617_4775__CCD-4775_modify_db_trigger_for_IU.sql",
            "V20260702_0000__CCD-6936_skip_case_pointers_in_logstash_queue.sql",
            "V20260917_0001__CCD-4262_widen_logstash_queue_id.sql",
            "V20260918_0000__CCD-4262_coalesce_logstash_queue_rows.sql",
            "V20260923_0000__CCD-4262_raise_logstash_queue_version.sql")) {
            Files.copy(MIGRATIONS.resolve(migration), migrationDirectory.resolve(migration));
        }
        flyway = Flyway.configure().dataSource(jdbc.getDataSource())
            .locations("filesystem:" + migrationDirectory.toAbsolutePath())
            .baselineOnMigrate(true).baselineVersion("1").outOfOrder(true)
            .callbacks(new LogstashQueueMigrationCallback()).load();
        flyway.migrate();
    }

    @Test
    void shouldNotRecordMarkerWhenClientContinuesAfterQueueCheckFails() throws Exception {
        insertCase(1);
        POSTGRES.copyFileToContainer(MountableFile.forHostPath(MARKER.toAbsolutePath()), "/tmp/marker.sql");
        // psql normally continues after an error: a separate INSERT would bypass the guard.
        var result = POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d",
            POSTGRES.getDatabaseName(), "-f", "/tmp/marker.sql");
        assertThat(result.getStderr()).contains("case_data_logstash_queue is not empty");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ccd_data_migration_status", Integer.class)).isZero();
        assertThat(markerColumnExists()).isTrue();
    }

    @Test
    void shouldBlockPopulatedDatabaseWithoutMarker() {
        insertCase(1);
        assertThatThrownBy(() -> migrate(DROP)).hasMessageContaining("CCD-4790 blocked");
        assertThat(markerColumnExists()).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM case_data_logstash_queue", Integer.class)).isEqualTo(1);
    }

    @Test
    void shouldDropColumnAfterMarkerAndContinueQueueingCaseChanges() throws Exception {
        insertCase(1);
        jdbc.execute("DELETE FROM case_data_logstash_queue");
        jdbc.execute(Files.readString(MARKER));
        Files.copy(MIGRATIONS.resolve(DROP), migrationDirectory.resolve(DROP));
        flyway.migrate();
        flyway.validate();
        assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history "
            + "WHERE version = '20260703.0000'", Boolean.class)).isTrue();
        assertThat(markerColumnExists()).isFalse();
        assertThat(jdbc.queryForObject("SELECT to_regprocedure('set_case_data_marked_by_logstash()') IS NULL",
            Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT to_regclass('idx_case_data_marked_by_logstash') IS NULL",
            Boolean.class)).isTrue();
        jdbc.execute("UPDATE case_data SET data = '{\"value\":2}' WHERE id = 1");
        insertCase(2);
        jdbc.execute("INSERT INTO case_data (id, data, state) VALUES (3, '{}', '')");
        assertThat(jdbc.queryForList("SELECT case_data_id FROM case_data_logstash_queue", Long.class))
            .containsExactlyInAnyOrder(1L, 2L);
        assertThat(jdbc.queryForList("SELECT id FROM case_data_logstash_queue", Long.class))
            .allSatisfy(id -> assertThat(id).isGreaterThan(10000000000L));
    }

    @Test
    void shouldAllowEmptyDatabaseWithoutMarker() throws Exception {
        migrate(DROP);
        assertThat(markerColumnExists()).isFalse();
        insertCase(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM case_data_logstash_queue", Integer.class)).isEqualTo(1);
    }

    @Test
    void shouldTimeOutOnBlockingReaderWithoutDroppingColumn() throws Exception {
        String sql = Files.readString(MIGRATIONS.resolve(DROP));
        try (Connection reader = jdbc.getDataSource().getConnection();
             Statement statement = reader.createStatement()) {
            reader.setAutoCommit(false);
            statement.execute("SELECT * FROM case_data");
            assertThatThrownBy(() -> new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()))
                .executeWithoutResult(status -> {
                    jdbc.execute("SET LOCAL statement_timeout = '20s'");
                    jdbc.execute(sql);
                })).hasMessageContaining("lock timeout");
            reader.rollback();
        }
        assertThat(markerColumnExists()).isTrue();
    }

    @Test
    void shouldExcludeMissingNullAndFalseSearchCriteriaFromCountsAndSamples() throws Exception {
        jdbc.execute("""
            INSERT INTO case_data (id, data, state) VALUES
            (1, '{}', 'Created'),
            (2, '{"SearchCriteria":null}', 'Created'),
            (3, '{"SearchCriteria":false}', 'Created'),
            (4, '{"SearchCriteria":{}}', 'Created'),
            (5, '{"SearchCriteria":{"SearchParties":[]}}', 'Created');
            """);
        String[] queries = Files.readString(Path.of("src/main/resources/db/useful-queries/"
            + "logstash_re_indexing_validation_query.sql")).split(";");
        assertThat(jdbc.queryForList(queries[2])).anySatisfy(row -> {
            assertThat(row.get("index_name")).isEqualTo("global_search");
            assertThat(row.get("expected_document_count")).isEqualTo(2L);
        });
        assertThat(jdbc.queryForList(queries[3]).stream()
            .filter(row -> "global_search".equals(row.get("index_name")))
            .map(row -> row.get("document_id")))
            .containsExactly(4L, 5L);
    }

    private void insertCase(long id) {
        jdbc.update("INSERT INTO case_data (id, data, state) VALUES (?, '{\"value\":1}', 'Created')", id);
    }

    private boolean markerColumnExists() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM information_schema.columns "
            + "WHERE table_schema = 'public' AND table_name = 'case_data' AND column_name = 'marked_by_logstash')",
            Boolean.class));
    }

    private void migrate(String name) throws Exception {
        String sql = Files.readString(MIGRATIONS.resolve(name));
        new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()))
            .executeWithoutResult(status -> jdbc.execute(sql));
    }
}
