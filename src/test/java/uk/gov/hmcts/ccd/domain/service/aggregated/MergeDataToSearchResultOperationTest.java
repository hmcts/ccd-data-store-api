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
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.model.definition.CaseDetails.LABEL_FIELD_TYPE;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.SearchResultBuilder.aSearchResult;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildData;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildSearchResultField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.aCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.aCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class MergeDataToSearchResultOperationTest {
    private static final String WORKBASKET_VIEW = "WORKBASKET";
    private static final String SEARCH_VIEW = "SEARCH";
    private static final String CASE_TYPE_ID = "VASE_TYPE";
    private static final String CASE_FIELD_1 = "Case field 1";
    private static final String CASE_FIELD_2 = "Case field 2";
    private static final String CASE_FIELD_3 = "Case field 3";

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
        CaseDetails caseDetails2 = new CaseDetails();
        caseDetails2.setReference(1000L);
        caseDetails2.setData(dataMap);
        caseDetailsList = Arrays.asList(caseDetails1, caseDetails2);

        final FieldType ftt = aFieldType().withType("Text").build();
        final FieldType ftl = aFieldType().withType(LABEL_FIELD_TYPE).build();

        caseType = aCaseType()
            .withCaseTypeId(CASE_TYPE_ID)
            .withField(aCaseField().withId(CASE_FIELD_1).withFieldType(ftt).build())
            .withField(aCaseField().withId(CASE_FIELD_2).withFieldType(ftt).build())
            .withField(aCaseField().withId(CASE_FIELD_3).withFieldType(ftl).build())
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


        final SearchResultView searchResultView = classUnderTest.execute(caseType, caseDetailsList, WORKBASKET_VIEW);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().length, is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().length, is(2))
        );
    }

    @Test
    @DisplayName("should get Search Results with defined columns")
    void getSearchView() {
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, CASE_FIELD_2))
            .build();

        doReturn(searchResult).when(uiDefinitionRepository).getSearchResult(CASE_TYPE_ID);

        final SearchResultView searchResultView = classUnderTest.execute(caseType, caseDetailsList, SEARCH_VIEW);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().length, is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().length, is(1))
        );
    }
}
