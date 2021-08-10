package uk.gov.hmcts.ccd;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableCaching
@EnableScheduling
@Profile("!NoCaching")
public class TestCacheAndScheduleConfiguration {
}
