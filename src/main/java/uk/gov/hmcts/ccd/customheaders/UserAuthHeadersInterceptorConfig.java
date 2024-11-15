package uk.gov.hmcts.ccd.customheaders;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.ccd.data.SecurityUtils;


public class UserAuthHeadersInterceptorConfig {

    @Bean
    public UserAuthHeadersInterceptor userAuthHeadersInterceptor(SecurityUtils securityUtils) {
        return new UserAuthHeadersInterceptor(securityUtils);
    }

    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }
}
