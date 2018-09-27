package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

class ElasticsearchQueryParserTest {

    private static final String FULL_QUERY = "{\"query\":{\"field\":\"value\"},\"sort\":{}}";
    private static final String NEW_QUERY = "{\"query\":{\"field\":\"value\",\"field1\":\"value1\"}}";
    private static final String NEW_FULL_QUERY = "{\"query\":{\"field\":\"value\",\"field1\":\"value1\"},\"sort\":{}}";

    @Mock
    private ObjectMapperService objectMapperService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ElasticsearchQueryParser queryParser;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        when(objectMapperService.convertStringToObject(FULL_QUERY, ObjectNode.class)).thenReturn(objectMapper.readValue(FULL_QUERY, ObjectNode.class));
        when(objectMapperService.convertStringToObject(NEW_QUERY, ObjectNode.class)).thenReturn(objectMapper.readValue(NEW_QUERY, ObjectNode.class));
        queryParser = new ElasticsearchQueryParser(objectMapperService, FULL_QUERY);
    }

    @Test
    @DisplayName("should extract query clause")
    void shouldExtractQueryClause() {
        assertThat(queryParser.extractQueryClause(), is("{\"field\":\"value\"}"));
    }

    @Test
    @DisplayName("should set new query clause")
    void shouldSetQueryClause() {
        queryParser.setQueryClause(NEW_QUERY);
        assertThat(queryParser.extractQueryClause(), is("{\"field\":\"value\",\"field1\":\"value1\"}"));
    }

    @Test
    @DisplayName("should return query string")
    void shouldReturnQueryString() {
        queryParser.setQueryClause(NEW_QUERY);
        assertThat(queryParser.getSearchQuery(), is(NEW_FULL_QUERY));
    }

    @Test
    @DisplayName("should throw exception when query node not found")
    void shouldThrowExceptionWhenQueryNodeNotFound() throws IOException {
        String query = "{}";
        when(objectMapperService.convertStringToObject(query, ObjectNode.class)).thenReturn(objectMapper.readValue(query, ObjectNode.class));
        ElasticsearchQueryParser queryParser = new ElasticsearchQueryParser(objectMapperService, query);

        assertThrows(ValidationException.class, queryParser::extractQueryClause);
    }
}
