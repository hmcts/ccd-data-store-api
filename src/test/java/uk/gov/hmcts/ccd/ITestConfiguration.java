package uk.gov.hmcts.ccd;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;

@Configuration
@Profile("itest")
class ITestConfiguration {

    @Bean
    @Primary
    UIDService uidService() {
        return Mockito.mock(UIDService.class);
    }
}
