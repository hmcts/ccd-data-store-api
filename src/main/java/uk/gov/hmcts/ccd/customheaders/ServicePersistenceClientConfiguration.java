package uk.gov.hmcts.ccd.customheaders;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;

@Configuration
public class ServicePersistenceClientConfiguration {

    @Bean
    public IdempotencyKeyRequestInterceptor idempotencyKeyRequestInterceptor(
        IdempotencyKeyHolder idempotencyKeyHolder) {
        return new IdempotencyKeyRequestInterceptor(idempotencyKeyHolder);
    }

    @Bean
    public UserAuthHeadersInterceptor userAuthHeadersInterceptor(
        SecurityUtils securityUtils) {
        return new UserAuthHeadersInterceptor(securityUtils);
    }
}
