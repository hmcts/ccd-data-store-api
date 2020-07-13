package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Singular;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;

import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static uk.gov.hmcts.ccd.ElasticsearchITConfiguration.INDEX_TYPE;
import static uk.gov.hmcts.ccd.ElasticsearchITConfiguration.INDICES;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.*;

public abstract class ElasticsearchBaseTest extends WireMockBaseTest {

    private static final String DATA_DIR = "elasticsearch/data";

    @BeforeAll
    public static void initElastic(@Autowired EmbeddedElastic embeddedElastic) throws IOException, InterruptedException {
        embeddedElastic.start();
        initData(embeddedElastic);
    }

    @AfterAll
    public static void tearDownElastic(@Autowired EmbeddedElastic embeddedElastic) {
        embeddedElastic.stop();
    }

    private static void initData(EmbeddedElastic embeddedElastic) throws IOException {
        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        for (String idx : INDICES) {
            Resource[] resources = resourceResolver.getResources(String.format("classpath:%s/%s/*.json", DATA_DIR, idx));
            for (Resource resource : resources) {
                String caseString = IOUtils.toString(resource.getInputStream(), UTF_8);
                embeddedElastic.index(idx, INDEX_TYPE, caseString);
            }
        }
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

        private final ObjectMapper objectMapper = new ObjectMapper();

        @JsonRawValue
        public String getQuery() {
            return query == null ? null : Strings.toString(query);
        }

        public String toJsonString() throws JsonProcessingException {
            return objectMapper.writeValueAsString(this);
        }
    }
}
