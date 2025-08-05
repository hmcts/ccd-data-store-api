package uk.gov.hmcts.ccd;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchMappings;

@Configuration
@EnableConfigurationProperties(ElasticsearchMappings.class)
@Slf4j
public class ElasticSearchConfiguration {

    private final ApplicationParams applicationParams;

    @Autowired
    public ElasticSearchConfiguration(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public JacksonJsonpMapper jsonpMapper(ObjectMapper objectMapper) {
        return new JacksonJsonpMapper(objectMapper);
    }

    @Bean(name = "DefaultObjectMapper")
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public ObjectMapper objectMapper() {
        return new Jackson2ObjectMapperBuilder()
            .featuresToEnable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .featuresToEnable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .featuresToEnable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .modulesToInstall(JavaTimeModule.class)
            .build();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    protected RestClientBuilder elasticsearchRestClientBuilder() {
        return RestClient.builder(
            new HttpHost(applicationParams.getElasticSearchHosts().getFirst(),
                9200, HttpHost.DEFAULT_SCHEME_NAME))
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
            .setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setDefaultIOReactorConfig(
                    IOReactorConfig.custom()
                        .setSoKeepAlive(true)
                        .build()
                )
            );
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClientBuilder elasticsearchRestClientBuilder) {
        RestClient restClient = elasticsearchRestClientBuilder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

}
