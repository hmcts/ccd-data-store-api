package uk.gov.hmcts.ccd.integrations;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.ContextCleanupListener;
import uk.gov.hmcts.ccd.PostgresUtil;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(locations = "classpath:integration_tests.properties")
@DirtiesContext
public abstract class IntegrationTest {

    @TestConfiguration
    static class Configuration extends ContextCleanupListener {

        private EmbeddedPostgres pg;

        @Bean
        @Primary
        public DataSource dataSource() throws IOException, SQLException {
            final PostgresUtil postgresUtil = new PostgresUtil();
            final EmbeddedPostgres pg = postgresUtil.embeddedPostgres();
            return postgresUtil.dataSource(pg);
        }

        @PreDestroy
        public void contextDestroyed() throws IOException {
            new PostgresUtil().contextDestroyed(pg);
        }
    }

}
