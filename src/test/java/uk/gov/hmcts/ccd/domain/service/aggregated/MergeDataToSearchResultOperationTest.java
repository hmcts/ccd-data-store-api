package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.STATE;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.SearchResultBuilder.aSearchResult;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildData;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildSearchResultField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.aCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;

class MergeDataToSearchResultOperationTest {
    private static final String SEARCH_VIEW = "SEARCH";
    private static final String CASE_TYPE_ID = "CASE_TYPE";
    private static final String CASE_FIELD_1 = "Case field 1";
    private static final String CASE_FIELD_2 = "Case field 2";
    private static final String CASE_FIELD_3 = "Case field 3";
    private static final String NO_ERROR = null;
    private static final String TIMEOUT_ERROR = "Error while retrieving drafts.";

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    private MergeDataToSearchResultOperation classUnderTest;

    private List<CaseDetails> caseDetailsList;
    private CaseType caseType;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        Map<String, JsonNode> dataMap = buildData(CASE_FIELD_1, CASE_FIELD_2, CASE_FIELD_3);

        CaseDetails caseDetails1 = new CaseDetails();
        caseDetails1.setReference(999L);
        caseDetails1.setData(dataMap);
        caseDetails1.setState("state1");
        CaseDetails caseDetails2 = new CaseDetails();
        caseDetails2.setReference(1000L);
        caseDetails2.setData(dataMap);
        caseDetails2.setState("state2");
        caseDetailsList = Arrays.asList(caseDetails1, caseDetails2);

        caseType = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID)
            .withField(aCaseField().withId(CASE_FIELD_1).build())
            .withField(aCaseField().withId(CASE_FIELD_2).build())
            .withField(aCaseField().withId(CASE_FIELD_3).build())
            .build();

        classUnderTest = new MergeDataToSearchResultOperation(uiDefinitionRepository);
    }

    @Test
    @DisplayName("should get Workbasket Results with defined columns")
    void getWorkbasketView() {
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, CASE_FIELD_1),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, CASE_FIELD_2))
            .build();

        doReturn(searchResult).when(uiDefinitionRepository).getWorkBasketResult(CASE_TYPE_ID);


        final SearchResultView searchResultView = classUnderTest.execute(caseType, caseDetailsList, WORKBASKET, NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(2)),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR)),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(0).getCaseFields().get(STATE.getReference()), is("state1")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFields().get(STATE.getReference()), is("state2")),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR))
        );
    }

    @Test
    @DisplayName("should get Search Results with defined columns")
    void getSearchView() {
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, CASE_FIELD_2))
            .build();

        doReturn(searchResult).when(uiDefinitionRepository).getSearchResult(CASE_TYPE_ID);

        final SearchResultView searchResultView = classUnderTest.execute(caseType, caseDetailsList, SEARCH_VIEW, TIMEOUT_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(1)),
            () -> assertThat(searchResultView.getResultError(), is(TIMEOUT_ERROR))
        );
    }
}
