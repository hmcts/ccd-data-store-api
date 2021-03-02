package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.NATIVE_ES_QUERY;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.QUERY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;

@ExtendWith(MockitoExtension.class)
class ElasticsearchCaseSearchRequestSecurityTest {

    private static final String CASE_TYPE_ID = "caseType";

    @Mock
    private CaseSearchFilter caseSearchFilter;
    @Mock
    private ObjectMapperService objectMapperService;
    @Mock
    private ObjectNode searchRequestJsonNode;

    private ElasticsearchCaseSearchRequestSecurity querySecurity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        querySecurity = new ElasticsearchCaseSearchRequestSecurity(Collections.singletonList(caseSearchFilter),
                objectMapperService);
        when(searchRequestJsonNode.has(QUERY)).thenReturn(true);
        when(searchRequestJsonNode.has(NATIVE_ES_QUERY)).thenReturn(false);
    }

    @Test
    @DisplayName("should parse and secure request with filters")
    void shouldSecureRequest() {
        CaseSearchRequest caseSearchRequest = mock(CaseSearchRequest.class);
        doReturn(CASE_TYPE_ID).when(caseSearchRequest).getCaseTypeId();
        doReturn("{}").when(caseSearchRequest).getQueryValue();
        doReturn("").when(caseSearchRequest).toJsonString();
        doReturn(Optional.of(mock(QueryBuilder.class))).when(caseSearchFilter).getFilter(CASE_TYPE_ID);
        doReturn(searchRequestJsonNode).when(objectMapperService).convertStringToObject(anyString(),
                eq(ObjectNode.class));
        doReturn(searchRequestJsonNode).when(searchRequestJsonNode).get(anyString());

        querySecurity.createSecuredSearchRequest(caseSearchRequest);

        assertAll(
            () -> verify(caseSearchRequest).getQueryValue(),
            () -> verify(caseSearchFilter).getFilter(CASE_TYPE_ID),
            () -> verify(caseSearchRequest).toJsonString(),
            () -> verify(searchRequestJsonNode).set(anyString(), any(JsonNode.class)),
            () -> verify(objectMapperService, times(2)).convertStringToObject(anyString(), eq(ObjectNode.class))
        );
    }
}
