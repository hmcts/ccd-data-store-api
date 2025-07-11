package uk.gov.hmcts.ccd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import uk.gov.hmcts.ccd.customheaders.ServicePersistenceClientConfiguration;

import java.time.Clock;

@SpringBootApplication
@EnableTransactionManagement(proxyTargetClass = true)
@EnableRetry
@ComponentScan(
    basePackages = { "uk.gov.hmcts.ccd" },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = ServicePersistenceClientConfiguration.class // This should not apply to all our feign clients.
    )
)
@EnableHypermediaSupport(type = { EnableHypermediaSupport.HypermediaType.HAL })
public class CoreCaseDataApplication {

    public static final String LOGGING_LEVEL_SPRINGFRAMEWORK = "logging.level.org.springframework.web";
    public static final String LOGGING_LEVEL_CCD = "logging.level.uk.gov.hmcts.ccd";

    protected CoreCaseDataApplication() {
    }

    @SuppressWarnings("checkstyle:CommentsIndentation") // commented out config predates
    public static void main(String[] args) {

        if (System.getProperty(LOGGING_LEVEL_CCD) != null) {
//          Configurator.setLevel(LOGGING_LEVEL_CCD,
//                                Level.valueOf(System.getProperty(LOGGING_LEVEL_CCD).toUpperCase()));
        }
        if (System.getProperty(LOGGING_LEVEL_SPRINGFRAMEWORK) != null) {
//          Configurator.setLevel(LOGGING_LEVEL_SPRINGFRAMEWORK,
//                                Level.valueOf(System.getProperty(LOGGING_LEVEL_SPRINGFRAMEWORK).toUpperCase()));
        }
        SpringApplication.run(CoreCaseDataApplication.class, args);
    }

    @Bean
    public Clock utcClock() {
        return Clock.systemUTC();
    }

}
