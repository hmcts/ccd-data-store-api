package uk.gov.hmcts.ccd.v2.internal.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.UseCase;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.UICaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseSearchResultViewResource;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UICaseSearchControllerTest {

    private static final String CASE_TYPE_ID = "GrantOnly";

    @Mock
    private CaseSearchOperation caseSearchOperation;

    @Mock
    private ElasticsearchQueryHelper elasticsearchQueryHelper;

    @InjectMocks
    private UICaseSearchController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldSearchCaseDetails() {
        CaseSearchResult caseSearchResult = mock(CaseSearchResult.class);
        CrossCaseTypeSearchRequest preparedRequest = mock(CrossCaseTypeSearchRequest.class);
        UICaseSearchResult uiCaseSearchResult = mock(UICaseSearchResult.class);
        when(elasticsearchQueryHelper.prepareRequest(any(), any(), any())).thenReturn(preparedRequest);
        when(caseSearchOperation.executeExternal(any(CrossCaseTypeSearchRequest.class))).thenReturn(caseSearchResult);
        when(caseSearchOperation.executeInternal(any(), any(), any())).thenReturn(uiCaseSearchResult);
        String searchRequest = "{\"query\": {\"match\": \"blah blah\"}}";
        List<String> caseTypeIds = singletonList(CASE_TYPE_ID);

        final ResponseEntity<CaseSearchResultViewResource> response = controller
            .searchCases(caseTypeIds, UseCase.WORKBASKET.getReference(), searchRequest);

        verify(elasticsearchQueryHelper).prepareRequest(eq(caseTypeIds), eq("WORKBASKET"), eq(searchRequest));
        verify(caseSearchOperation).executeExternal(eq(preparedRequest));
        verify(caseSearchOperation).executeInternal(eq(caseSearchResult), eq(caseTypeIds), eq(UseCase.WORKBASKET));
        assertAll(
            () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
            () -> assertThat(response.getBody().getHeaders(), is(uiCaseSearchResult.getHeaders())),
            () -> assertThat(response.getBody().getCases(), is(uiCaseSearchResult.getCases())),
            () -> assertThat(response.getBody().getTotal(), is(uiCaseSearchResult.getTotal()))
        );
    }
}
