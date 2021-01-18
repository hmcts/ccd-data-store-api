package uk.gov.hmcts.ccd;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Lightweight test bootstrap to test integration of the persistence layer.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class PersistenceIT {

    @TestConfiguration
    static class Configuration {
        @Bean
        public EmbeddedPostgres embeddedPostgres() throws IOException {
            return EmbeddedPostgres
                .builder()
                .setPort(0)
                .start();
        }

        @Bean
        public DataSource dataSource() throws IOException, SQLException {
            final EmbeddedPostgres pg = embeddedPostgres();

            final Properties props = new Properties();
            // Instruct JDBC to accept JSON string for JSONB
            props.setProperty("stringtype", "unspecified");
            final Connection connection = DriverManager.getConnection(pg.getJdbcUrl("postgres", "postgres"), props);
            return new SingleConnectionDataSource(connection, true);
        }

        @PreDestroy
        public void contextDestroyed() throws IOException {
            embeddedPostgres().close();
        }
    }
}
