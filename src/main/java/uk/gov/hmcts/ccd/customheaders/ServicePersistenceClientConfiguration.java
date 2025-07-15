package uk.gov.hmcts.ccd.customheaders;

import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;

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
