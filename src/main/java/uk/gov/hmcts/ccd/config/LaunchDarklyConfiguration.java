package uk.gov.hmcts.ccd.config;

import com.launchdarkly.client.LDClient;
import com.launchdarkly.client.LDUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@Configuration
public class LaunchDarklyConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(LaunchDarklyConfiguration.class);

    private LDClient client;
    private LDUser user;

    @Value("${launchdarkly.sdk.key}")
    private String sdkKey;

    @Value("${launchdarkly.user.key}")
    private String userKey;

    @Value("${launchdarkly.component}")
    private String component;

    @PostConstruct
    void LaunchDarkly() {
        LOG.info("LAUNCHDARKLY CLIENT INITIALISED WITH: " + sdkKey);
        client = new LDClient(sdkKey);
        user = new LDUser.Builder(userKey)
            .custom("component", component)
            .build();
    }

    @Bean
    public LDClient ldClient() {
        return client;
    }

    @Bean
    public LDUser ldUser() {
        return user;
    }

    @PreDestroy
    void close() throws IOException {
        client.flush();
        client.close();
    }
}
