package uk.gov.hmcts.ccd;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.ContextCleanupListener;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@Configuration
@Profile("itest")
class ITestConfiguration extends ContextCleanupListener {

    private final PostgresUtil postgresUtil;

    private EmbeddedPostgres pg;

    @Autowired
    ITestConfiguration(final PostgresUtil postgresUtil) {
        this.postgresUtil = postgresUtil;
    }

    @Bean
    DataSource dataSource() throws IOException, SQLException {
        pg = postgresUtil.embeddedPostgres();
        return postgresUtil.dataSource(pg);
    }

    @PreDestroy
    void contextDestroyed() throws IOException {
        postgresUtil.contextDestroyed(pg);
    }

    @Bean
    @Primary
    UIDService uidService() {
        return Mockito.mock(UIDService.class);
    }
}
