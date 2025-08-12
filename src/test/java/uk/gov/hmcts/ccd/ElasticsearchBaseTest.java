package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.SORT;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.SOURCE;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public abstract class ElasticsearchBaseTest extends WireMockBaseTest {

    protected static final ElasticsearchContainer elasticsearchContainer;

    protected static ElasticsearchITSetup configurer;

    static  {
        elasticsearchContainer
            = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.0.4")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("xpack.security.transport.ssl.enabled", "false")
            .withExposedPorts(9200);

        elasticsearchContainer.setWaitStrategy(new HttpWaitStrategy()
            .forPath("/_cluster/health")
            .forPort(9200)
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofMinutes(3)));

        elasticsearchContainer.start();
    }

    @BeforeAll
    void initElasticSearch() throws IOException {
        int mappedPort = elasticsearchContainer.getMappedPort(9200);
        log.info("Elasticsearch started on port {}", mappedPort);

        configurer = new ElasticsearchITSetup(mappedPort);
        log.info("Elasticsearch adding indexes.");
        configurer.initIndexes();
        log.info("Elasticsearch adding data.");
        configurer.initData();
    }

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        log.info("Elasticsearch setting search host and port properties.");
        registry.add("search.elastic.hosts", elasticsearchContainer::getHost);
        registry.add("search.elastic.port", () -> elasticsearchContainer.getMappedPort(9200));
    }

    public ElasticsearchTestRequest caseReferenceRequest(String caseReference) {
        return ElasticsearchTestRequest.builder()
            .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), caseReference))
            .build();
    }

    public ElasticsearchTestRequest matchAllRequest() {
        return ElasticsearchTestRequest.builder()
            .query(matchAllQuery())
            .build();
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ElasticsearchTestRequest {

        private QueryBuilder query;
        @Singular
        @JsonProperty(SOURCE)
        private List<String> sources;
        @Singular
        @JsonProperty(SORT)
        private List<Object> sorts;
        @JsonProperty
        private Integer size;
        @JsonProperty
        private Integer from;
        @JsonIgnore
        private List<String> supplementaryData;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @JsonRawValue
        public String getQuery() {
            return query == null ? null : Strings.toString(query);
        }

        public String toJsonString() throws JsonProcessingException {
            if (supplementaryData == null) {
                return objectMapper.writeValueAsString(this);
            }

            return toJsonStringWithSupplementaryData();
        }

        private String toJsonStringWithSupplementaryData() throws JsonProcessingException {
            ArrayNode supplementaryDataNode = objectMapper.createArrayNode();
            supplementaryData.forEach(sd -> supplementaryDataNode.add(new TextNode(sd)));
            ObjectNode request = objectMapper.createObjectNode();
            request.set("native_es_query", objectMapper.readTree(objectMapper.writeValueAsString(this)));
            request.set("supplementary_data", supplementaryDataNode);
            return request.toString();
        }
    }
}
