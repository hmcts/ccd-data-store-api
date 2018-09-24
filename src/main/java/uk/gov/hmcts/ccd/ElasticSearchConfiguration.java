package uk.gov.hmcts.ccd;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class ElasticSearchConfiguration {

    @Autowired
    private ApplicationParams applicationParams;

    @Bean
    public JestClient jestClient() {

        JestClientFactory factory = new JestClientFactory();
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(applicationParams.getElasticSearchHosts())
            .multiThreaded(true)
            .maxConnectionIdleTime(5, TimeUnit.SECONDS)
            .gson(gson).build());
        return factory.getObject();
    }
}
