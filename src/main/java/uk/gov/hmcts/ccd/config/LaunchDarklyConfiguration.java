package uk.gov.hmcts.ccd.config;

import com.launchdarkly.client.LDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@Configuration
public class LaunchDarklyConfiguration {

    private LDClient client;

    @Value("${launchdarkly.sdk.key}")
    private String sdkKey;

    @PostConstruct
    void LaunchDarkly() {
        client = new LDClient(sdkKey);
    }

    @Bean
    public LDClient ldClient() {
        return client;
    }

    @PreDestroy
    void close() throws IOException {
        client.flush();
        client.close();
    }
}
