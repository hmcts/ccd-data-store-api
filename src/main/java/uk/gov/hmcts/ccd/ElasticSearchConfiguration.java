package uk.gov.hmcts.ccd;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchMappings;

@Configuration
@EnableConfigurationProperties(ElasticsearchMappings.class)
public class ElasticSearchConfiguration {

    private final ApplicationParams applicationParams;

    @Bean(name = "DefaultObjectMapper")
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public JsonpMapper jsonpMapper(ObjectMapper objectMapper) {
        return new JacksonJsonpMapper(objectMapper);
    }

    @Autowired
    public ElasticSearchConfiguration(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        //TODO: Replace with applicationParams.getElasticsearchHost() and port
        RestClientBuilder builder = RestClient.builder(
            new HttpHost("localhost", 9200))
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                    .setConnectTimeout(5000)
                    .setSocketTimeout(60000))
            .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                .setDefaultIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(true).build())
            );
        RestClient restClient = builder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

}
