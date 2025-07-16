package uk.gov.hmcts.ccd.feign;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FeignClientConfig {

    @Value("${lau.remote.delayOnError:20}")
    private long initialDelay;

    @Value("${lau.remote.maxDelay:100}")
    private long maxDelay;

    private static final int MAX_RETRIES = 3;

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(
                initialDelay,maxDelay,MAX_RETRIES
        );
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }
}
