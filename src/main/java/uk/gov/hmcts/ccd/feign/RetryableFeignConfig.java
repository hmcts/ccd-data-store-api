package uk.gov.hmcts.ccd.feign;

import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
public class RetryableFeignConfig {

    @Value("${delayOnError:3}")
    long initialDelay;
    long maxDelay = 20;
    int maxRetries = 3;

    @Bean
    public Retryer feignRetryer() {
        // Default backoff calculated using the following formula:
        // Math.min(period * Math.pow(1.5, attempt - 1), maxPeriod)
        // First retry will be after 30s, second after 45s, third after 67.5s
        return new Retryer.Default(
            SECONDS.toMillis(initialDelay),
            SECONDS.toMillis(maxDelay),
            maxRetries);
    }
}
