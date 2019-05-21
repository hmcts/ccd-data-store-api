package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.STATE;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.LABEL;
import static uk.gov.hmcts.ccd.domain.service.aggregated.MergeDataToSearchResultOperation.WORKBASKET_VIEW;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.SearchResultBuilder.aSearchResult;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildData;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildSearchResultField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;

class MergeDataToSearchResultOperationTest {
    private static final String SEARCH_VIEW = "SEARCH";
    private static final String CASE_TYPE_ID = "CASE_TYPE";
    private static final String CASE_FIELD_1 = "Case field 1";
    private static final String CASE_FIELD_2 = "Case field 2";
    private static final String CASE_FIELD_3 = "Case field 3";
    private static final String CASE_FIELD_4 = "Case field 4";
    private static final String CASE_FIELD_5 = "Case field 5";
    private static final String ROLE_IN_USER_ROLE_1 = "Role 1";
    private static final String ROLE_IN_USER_ROLE_2 = "Role 2";
    private static final String ROLE_NOT_IN_USER_ROLE = "Role X";
    private static final String LABEL_ID = "LabelId";
    private static final String LABEL_TEXT = "LabelText";
    private static final String NO_ERROR = null;
    private static final String TIMEOUT_ERROR = "Error while retrieving drafts.";

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @Mock
    private UserRepository userRepository;
    private MergeDataToSearchResultOperation classUnderTest;

    private List<CaseDetails> caseDetailsList;
    private CaseType caseType;
    private CaseType caseTypeWithLabels;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        Map<String, JsonNode> dataMap = buildData(CASE_FIELD_1, CASE_FIELD_2, CASE_FIELD_3, CASE_FIELD_4);

        CaseDetails caseDetails1 = new CaseDetails();
        caseDetails1.setReference(999L);
        caseDetails1.setData(dataMap);
        caseDetails1.setState("state1");
        CaseDetails caseDetails2 = new CaseDetails();
        caseDetails2.setReference(1000L);
        caseDetails2.setData(dataMap);
        caseDetails2.setState("state2");
        caseDetailsList = Arrays.asList(caseDetails1, caseDetails2);

        final FieldType ftt = aFieldType().withType("Text").build();

        caseType = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(ftt).build())
            .withField(newCaseField().withId(CASE_FIELD_2).withFieldType(ftt).build())
            .withField(newCaseField().withId(CASE_FIELD_3).withFieldType(ftt).build())
            .withField(newCaseField().withId(CASE_FIELD_4).withFieldType(ftt).build())
            .withField(newCaseField().withId(CASE_FIELD_5).withFieldType(ftt).build())
            .build();

        final CaseField labelField = buildLabelCaseField(LABEL_ID, LABEL_TEXT);
        caseTypeWithLabels = newCaseType()
            .withCaseTypeId(CASE_TYPE_ID)
            .withField(newCaseField().withId(CASE_FIELD_1).withFieldType(ftt).build())
            .withField(newCaseField().withId(CASE_FIELD_2).withFieldType(ftt).build())
            .withField(newCaseField().withId(CASE_FIELD_3).withFieldType(ftt).build())
            .withField(labelField)
            .build();
        doReturn(Collections.emptySet()).when(userRepository).getUserRoles();
        classUnderTest = new MergeDataToSearchResultOperation(uiDefinitionRepository, userRepository);
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
            () -> assertThat(searchResultView.getSearchResultViewItems().get(0).getCaseFields().get(STATE.getReference()), is("state1")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFields().get(STATE.getReference()), is("state2")),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR))
        );
    }

    @Test
    @DisplayName("should get Workbasket Results with defined columns and allowed roles")
    void getWorkbasketViewWithValidRoles() {
        SearchResultField searchResultFieldWithValidRole = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_4, CASE_FIELD_4);
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_1);
        SearchResultField searchResultFieldWithInvalidRole = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_5, CASE_FIELD_5);
        searchResultFieldWithInvalidRole.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, CASE_FIELD_1),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, CASE_FIELD_2),
                searchResultFieldWithValidRole,
                searchResultFieldWithInvalidRole)
            .build();

        doReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1)).when(userRepository).getUserRoles();
        doReturn(searchResult).when(uiDefinitionRepository).getWorkBasketResult(CASE_TYPE_ID);


        final SearchResultView searchResultView = classUnderTest.execute(caseType, caseDetailsList, WORKBASKET, NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(3)),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(0).getCaseFields().get(STATE.getReference()), is("state1")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFields().get(STATE.getReference()), is("state2")),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(0).getCaseFieldId(), is(CASE_FIELD_1)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(1).getCaseFieldId(), is(CASE_FIELD_2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(2).getCaseFieldId(), is(CASE_FIELD_4)),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR))
        );
    }

    @Test
    @DisplayName("should get Workbasket Results with defined columns for allowed roles and no duplicate columns")
    void getWorkbasketViewWithValidRolesAndNoDuplicateColumns() {
        SearchResultField searchResultFieldWithValidRole = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_4, CASE_FIELD_4);
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_1);
        SearchResultField searchResultFieldWithValidRole2 = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_4, CASE_FIELD_4);
        searchResultFieldWithValidRole2.setRole(ROLE_IN_USER_ROLE_2);
        SearchResultField searchResultFieldWithInvalidRole = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_5, CASE_FIELD_5);
        searchResultFieldWithInvalidRole.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, CASE_FIELD_1),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, CASE_FIELD_1),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, CASE_FIELD_1),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, CASE_FIELD_2),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, CASE_FIELD_2),
                searchResultFieldWithValidRole,
                searchResultFieldWithValidRole2,
                searchResultFieldWithInvalidRole)
            .build();

        doReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2)).when(userRepository).getUserRoles();
        doReturn(searchResult).when(uiDefinitionRepository).getWorkBasketResult(CASE_TYPE_ID);


        final SearchResultView searchResultView = classUnderTest.execute(caseType, caseDetailsList, WORKBASKET, NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(3)),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(0).getCaseFields().get(STATE.getReference()), is("state1")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFields().get(STATE.getReference()), is("state2")),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(0).getCaseFieldId(), is(CASE_FIELD_1)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(1).getCaseFieldId(), is(CASE_FIELD_2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(2).getCaseFieldId(), is(CASE_FIELD_4)),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR))
        );
    }

    @Test
    @DisplayName("should process result fields with valid and invalid roles correctly")
    void getWorkbasketViewWithValidRolesAndInvalidRoles() {
        SearchResultField searchResultFieldWithInvalidRole1 = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_4, CASE_FIELD_4);
        searchResultFieldWithInvalidRole1.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResultField searchResultFieldWithValidRole = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_4, CASE_FIELD_4);
        searchResultFieldWithValidRole.setRole(ROLE_IN_USER_ROLE_2);
        SearchResultField searchResultFieldWithInvalidRole2 = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_5, CASE_FIELD_5);
        searchResultFieldWithInvalidRole2.setRole(ROLE_NOT_IN_USER_ROLE);
        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, CASE_FIELD_1),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, CASE_FIELD_1),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, CASE_FIELD_2),
                searchResultFieldWithInvalidRole1,
                searchResultFieldWithInvalidRole2,
                searchResultFieldWithValidRole)
            .build();

        doReturn(Sets.newHashSet(ROLE_IN_USER_ROLE_1, ROLE_IN_USER_ROLE_2)).when(userRepository).getUserRoles();
        doReturn(searchResult).when(uiDefinitionRepository).getWorkBasketResult(CASE_TYPE_ID);


        final SearchResultView searchResultView = classUnderTest.execute(caseType, caseDetailsList, WORKBASKET, NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(3)),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(0).getCaseFields().get(STATE.getReference()), is("state1")),
            () -> assertThat(searchResultView.getSearchResultViewItems().get(1).getCaseFields().get(STATE.getReference()), is("state2")),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(0).getCaseFieldId(), is(CASE_FIELD_1)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(1).getCaseFieldId(), is(CASE_FIELD_2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().get(2).getCaseFieldId(), is(CASE_FIELD_4)),
            () -> assertThat(searchResultView.getResultError(), is(NO_ERROR))
        );
    }

    @Test
    @DisplayName("should get Workbasket Results with defined columns and Labels")
    void getWorkbasketViewAndLabels() {

        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_1, CASE_FIELD_1),
                buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, CASE_FIELD_2))
            .build();

        doReturn(searchResult).when(uiDefinitionRepository).getWorkBasketResult(CASE_TYPE_ID);

        final SearchResultView searchResultView = classUnderTest.execute(caseTypeWithLabels,
                                                                         caseDetailsList,
                                                                         WORKBASKET_VIEW,
                                                                         NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(2)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(0)
                .getCaseFields()
                .get(LABEL_ID)).asText(), is(LABEL_TEXT)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(1)
                .getCaseFields()
                .get(LABEL_ID)).asText(), is(LABEL_TEXT)));
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

    @Test
    @DisplayName("should get Search Results with defined columns and labels")
    void getSearchViewAndLabels() {

        SearchResult searchResult = aSearchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, CASE_FIELD_2))
            .build();

        doReturn(searchResult).when(uiDefinitionRepository).getSearchResult(CASE_TYPE_ID);

        final SearchResultView searchResultView = classUnderTest.execute(caseTypeWithLabels,
                                                                         caseDetailsList,
                                                                         SEARCH_VIEW,
                                                                         NO_ERROR);
        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(2)),
            () -> assertThat(searchResultView.getSearchResultViewColumns().size(), is(1)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(0).getCaseFields()
                    .get(LABEL_ID)).asText(), is(LABEL_TEXT)),
            () -> assertThat(((TextNode)searchResultView.getSearchResultViewItems().get(1).getCaseFields()
                    .get(LABEL_ID)).asText(), is(LABEL_TEXT)))
        ;
    }

    private CaseField buildLabelCaseField(final String labelId, final String labelText) {
        final CaseField caseField = newCaseField()
            .withId(labelId)
            .withFieldType(aFieldType()
                .withType(LABEL)
                .withId(UUID.randomUUID().toString())
                .build())
            .withFieldLabelText(labelText)
            .build();
        return caseField;
    }
}
