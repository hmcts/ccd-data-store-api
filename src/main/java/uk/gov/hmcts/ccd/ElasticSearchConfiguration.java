package uk.gov.hmcts.ccd;

import java.util.concurrent.TimeUnit;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchMappings;

@Configuration
@EnableConfigurationProperties(ElasticsearchMappings.class)
public class ElasticSearchConfiguration {

    private final ApplicationParams applicationParams;

    @Autowired
    public ElasticSearchConfiguration(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Bean
    public JestClient jestClient() {

        JestClientFactory factory = new JestClientFactory();
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(applicationParams.getElasticSearchDataHosts())
                                        .multiThreaded(true)
                                        .maxConnectionIdleTime(15, TimeUnit.SECONDS)
                                        .connTimeout(4000)
                                        .readTimeout(applicationParams.getElasticSearchRequestTimeout())
                                        .gson(gson)
                                        .discoveryEnabled(applicationParams.isElasticsearchNodeDiscoveryEnabled())
                                        .discoveryFrequency(
                                            applicationParams.getElasticsearchNodeDiscoveryFrequencyMillis(),
                                            TimeUnit.MILLISECONDS)
                                        .discoveryFilter(applicationParams.getElasticsearchNodeDiscoveryFilter())
                                        .build());
        return factory.getObject();
    }

}
