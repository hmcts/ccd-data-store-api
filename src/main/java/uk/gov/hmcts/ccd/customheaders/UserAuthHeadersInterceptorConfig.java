package uk.gov.hmcts.ccd.customheaders;

import feign.Retryer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;

@Configuration
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
