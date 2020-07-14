package uk.gov.hmcts.ccd.integrations;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(locations = "classpath:integration_tests.properties")
@DirtiesContext
public abstract class IntegrationTest {

    // FIXME : enable this based on profile and also IdamIT is fixed.

//    @TestConfiguration
//    static class Configuration extends ContextCleanupListener {
//
//        private EmbeddedPostgres pg;
//
//        @Bean
//        public DataSource dataSource() throws IOException, SQLException {
//            final PostgresUtil postgresUtil = new PostgresUtil();
//            final EmbeddedPostgres pg = postgresUtil.embeddedPostgres();
//            return postgresUtil.dataSource(pg);
//        }
//
//        @PreDestroy
//        public void contextDestroyed() throws IOException {
//            new PostgresUtil().contextDestroyed(pg);
//        }
//    }

}
