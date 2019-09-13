package uk.gov.hmcts.ccd.data.helper;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@TestConfiguration
public class AccessManagementQueryHelper {

    private static final Logger LOG = LoggerFactory.getLogger(AccessManagementQueryHelper.class);

    private static DataSource dataSource;
    private static EmbeddedPostgres pg;

    public Integer findExplicitAccessPermissions(String jurisdiction) {
        String query = ("select COUNT (*) from access_management where service_name = ?");
        return new JdbcTemplate(dataSource).queryForObject(query, Integer.class, jurisdiction);
    }

    public void deleteAllFromAccessManagementTables() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String query = ("delete from access_management;" +
            "delete from roles;" +
            "delete from resources;" +
            "delete from resource_attributes;" +
            "delete from default_permissions_for_roles;" +
            "delete from services;");
        jdbcTemplate.update(query);
    }

    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres
            .builder()
            .setPort(0)  // use next available port
            .start();
    }

    public DataSource dataSource(final EmbeddedPostgres pg) throws SQLException {
        final Properties props = new Properties();
        // Instruct JDBC to accept JSON string for JSONB
        props.setProperty("stringtype", "unspecified");
        final Connection connection = DriverManager.getConnection(pg.getJdbcUrl("postgres", "postgres"), props);
        LOG.info("Started Postgres, port number = {}", pg.getPort());
        return new SingleConnectionDataSource(connection, true);
    }

    private void contextDestroyed(final EmbeddedPostgres pg) throws IOException {
        if (null != pg) {
            LOG.info("Closing down Postgres, port number = {}", pg.getPort());
            pg.close();
        }
    }

    @Bean("amDataSource")
    DataSource dataSource() throws IOException, SQLException {
        pg = embeddedPostgres();
        dataSource = dataSource(pg);
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("db/migration")
            .load();

        flyway.migrate();
        return dataSource(pg);
    }

    @PreDestroy
    public void contextDestroyed() throws IOException {
        contextDestroyed(pg);
    }
}
