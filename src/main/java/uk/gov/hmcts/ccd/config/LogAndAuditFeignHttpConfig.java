package uk.gov.hmcts.ccd.config;

import feign.Client;
import feign.Feign;
import feign.Request;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.auditlog.LogAndAuditFeignClient;
import uk.gov.hmcts.ccd.feign.FeignErrorDecoder;

import java.util.concurrent.TimeUnit;

@Configuration
public class LogAndAuditFeignHttpConfig {

    private final ErrorDecoder errorDecoder;

    private final Client httpClient;

    private final Encoder encoder;

    private final Request.Options options;

    @Autowired
    public LogAndAuditFeignHttpConfig(
        FeignErrorDecoder errorDecoder,
        Client httpClient,
        Encoder encoder,
        @Value("${lau.http-connection-timeout-seconds}") Integer connectionTimeoutSecs,
        @Value("${lau.http-read-timeout-seconds}") Integer readTimeoutSecs) {
        this.errorDecoder = errorDecoder;
        this.httpClient = httpClient;
        this.encoder = encoder;
        this.options = new Request.Options(
            (int) TimeUnit.SECONDS.toMillis(connectionTimeoutSecs),
            (int) TimeUnit.SECONDS.toMillis(readTimeoutSecs),
            false);
    }

    @Bean(name = "LogAuditFeignClient")
    public LogAndAuditFeignClient logAndAuditFeignClient(@Value("${lau.remote.case.audit.url}") String target) {
        return buildLogAuditFeignClient(LogAndAuditFeignClient.class, target);
    }

    private <T> T buildLogAuditFeignClient(Class<T> clazz, String target) {
        return Feign.builder()
            .errorDecoder(errorDecoder)
            .options(options)
            .client(httpClient)
            .encoder(encoder)
            .logger(new Slf4jLogger(clazz))
            .target(clazz, target);
    }

}

