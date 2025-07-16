package uk.gov.hmcts.ccd.feign;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
@Slf4j
public class FeignClientConfig {

    @Value("${lau.remote.delayOnError:3}")
    private long initialDelay;

    private static final long MAX_DELAY = 10;
    private static final int MAX_RETRIES = 3;

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(
                SECONDS.toMillis(initialDelay),
                SECONDS.toMillis(MAX_DELAY),
                MAX_RETRIES);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }
}
