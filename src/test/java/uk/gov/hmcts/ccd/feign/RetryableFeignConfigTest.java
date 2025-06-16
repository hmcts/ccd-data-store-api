package uk.gov.hmcts.ccd.feign;

import feign.Retryer;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class RetryableFeignConfigTest {

    @Test
    void shouldProvideRetryerBean() {
        try (AnnotationConfigApplicationContext context = new
            AnnotationConfigApplicationContext(RetryableFeignConfig.class)) {
            Retryer retryer = context.getBean(Retryer.class);
            assertThat(retryer).isNotNull();
        }
    }
}
