package uk.gov.hmcts.ccd;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@Component
public class PostgresUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresUtil.class);

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

    public void contextDestroyed(final EmbeddedPostgres pg) throws IOException {
        if (null != pg) {
            LOG.info("Closing down Postgres, port number = {}", pg.getPort());
            pg.close();
        }
    }
}
