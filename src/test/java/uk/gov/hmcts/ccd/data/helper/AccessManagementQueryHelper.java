package uk.gov.hmcts.ccd.data.helper;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;

@TestConfiguration
public class AccessManagementQueryHelper {
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

    @Bean("amDataSource")
    public static DataSource amDataSource() throws IOException {
        dataSource = getPostgres().getPostgresDatabase();

        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("db/migration")
            .load();

        flyway.migrate();
        return dataSource;
    }

    private static EmbeddedPostgres getPostgres() throws IOException {
        if (pg == null) {
            pg = EmbeddedPostgres.builder().start();
        }
        return pg;
    }

    public void closePostgres() {
        try {
            if (pg != null) {
                pg.close();
            }
        } catch (IOException e) {

        }
    }
}
