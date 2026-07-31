package uk.gov.hmcts.ccd;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchMappings;

import java.util.Arrays;

@Configuration
@EnableConfigurationProperties(ElasticsearchMappings.class)
@Slf4j
public class ElasticSearchConfiguration {

    private final ApplicationParams applicationParams;

    @Autowired
    public ElasticSearchConfiguration(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Bean(name = "ElasticsearchObjectMapper")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public JsonMapper objectMapper() {
        return JsonMapper.builderWithJackson2Defaults()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public Jackson3JsonpMapper jsonpMapper(
        @Qualifier("ElasticsearchObjectMapper") JsonMapper objectMapper
    ) {
        return new Jackson3JsonpMapper(objectMapper);
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(
        @Qualifier("ElasticsearchObjectMapper") JsonMapper objectMapper
    ) {
        HttpHost[] esHosts = applicationParams.getElasticSearchDataHosts().stream()
            .map(HttpHost::create)
            .toArray(HttpHost[]::new);
        Arrays.stream(esHosts).forEach(host -> log.info("ES Host: {}", host));

        RestClientBuilder builder = RestClient.builder(esHosts)
            .setFailureListener(new RestClient.FailureListener() {
                @Override
                public void onFailure(Node node) {
                    log.warn("Node marked as dead: {}", node);
                }
            })
            .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS)
            .setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                    .setConnectTimeout(5000)
                    .setSocketTimeout(60000)
            )
            .setHttpClientConfigCallback(this::customizeHttpClient);

        RestClient restClient = builder.build();

        ElasticsearchTransport transport = new RestClientTransport(
            restClient,
            new Jackson3JsonpMapper(objectMapper)
        );

        return new ElasticsearchClient(transport);
    }

    private HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
        return httpClientBuilder
            .setDefaultIOReactorConfig(
                IOReactorConfig.custom()
                    .setSoTimeout(applicationParams.getElasticSearchRequestTimeout())
                    .build()
            );
    }
}
