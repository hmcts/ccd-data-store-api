package uk.gov.hmcts.ccd.v2.internal.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView;
import uk.gov.hmcts.ccd.domain.service.search.CaseSearchResultViewGenerator;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchSortService;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseSearchResultViewResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;

class UICaseSearchControllerTest {

    private static final String CASE_TYPE_ID = "GrantOnly";

    @Mock
    private CaseSearchOperation caseSearchOperation;

    @Mock
    private ElasticsearchQueryHelper elasticsearchQueryHelper;

    @Mock
    private CaseSearchResultViewGenerator caseSearchResultViewGenerator;

    @Mock
    private ElasticsearchSortService elasticsearchSortService;

    @InjectMocks
    private UICaseSearchController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldSearchCaseDetails() throws JsonProcessingException {
        CaseSearchResult caseSearchResult = mock(CaseSearchResult.class);
        CaseSearchResultView caseSearchResultView = mock(CaseSearchResultView.class);
        String searchRequest = "{\"query\": {\"match\": \"blah blah\"}}";
        JsonNode searchRequestNode = new ObjectMapper().readTree(searchRequest);
        ElasticsearchRequest elasticSearchRequest = new ElasticsearchRequest(searchRequestNode);
        when(elasticsearchQueryHelper.validateAndConvertRequest(any())).thenReturn(elasticSearchRequest);
        when(caseSearchOperation.execute(any(CrossCaseTypeSearchRequest.class))).thenReturn(caseSearchResult);
        when(caseSearchResultViewGenerator.execute(any(), any(), any(), any())).thenReturn(caseSearchResultView);

        final ResponseEntity<CaseSearchResultViewResource> response = controller
            .searchCases(CASE_TYPE_ID, WORKBASKET, searchRequest);

        verify(elasticsearchQueryHelper).validateAndConvertRequest(eq(searchRequest));
        verify(caseSearchOperation).execute(argThat(crossCaseTypeSearchRequest -> {
            assertThat(crossCaseTypeSearchRequest.getSearchRequestJsonNode(), is(searchRequestNode));
            assertThat(crossCaseTypeSearchRequest.getCaseTypeIds().size(), is(1));
            assertThat(crossCaseTypeSearchRequest.getCaseTypeIds().get(0), is(CASE_TYPE_ID));
            assertThat(crossCaseTypeSearchRequest.isMultiCaseTypeSearch(), is(false));
            assertThat(crossCaseTypeSearchRequest.getAliasFields().size(), is(0));
            return true;
        }));
        verify(caseSearchResultViewGenerator).execute(eq(CASE_TYPE_ID), eq(caseSearchResult), eq(WORKBASKET),
            eq(Collections.emptyList()));
        assertAll(
            () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
            () -> assertThat(response.getBody().getHeaders(), is(caseSearchResultView.getHeaders())),
            () -> assertThat(response.getBody().getCases(), is(caseSearchResultView.getCases())),
            () -> assertThat(response.getBody().getTotal(), is(caseSearchResultView.getTotal()))
        );
    }

    @Test
    void shouldBuildCaseIdList() {
        CaseSearchResultViewResource resource = mock(CaseSearchResultViewResource.class);
        ResponseEntity<CaseSearchResultViewResource> response = ResponseEntity.ok(resource);
        when(resource.getCases()).thenReturn(searchResultViewItems(3));

        final String caseIds = UICaseSearchController.buildCaseIds(response);

        assertAll(
            () -> assertThat(caseIds, is("1,2,3"))
        );
    }

    @Test
    void shouldBuildCaseIdListWithCappedLimit() {
        CaseSearchResultViewResource resource = mock(CaseSearchResultViewResource.class);
        ResponseEntity<CaseSearchResultViewResource> response = ResponseEntity.ok(resource);
        when(resource.getCases()).thenReturn(searchResultViewItems(20));

        final String caseIds = UICaseSearchController.buildCaseIds(response);

        assertAll(
            () -> assertThat(caseIds, is("1,2,3,4,5,6,7,8,9,10"))
        );
    }

    private List<SearchResultViewItem> searchResultViewItems(int numberOfEntries) {
        List<SearchResultViewItem> items = new ArrayList<>();
        IntStream.range(1, numberOfEntries + 1).forEach(idx ->
            items.add(new SearchResultViewItem(Integer.toString(idx), emptyMap(), emptyMap(), emptyMap()))
        );
        return items;
    }
}
