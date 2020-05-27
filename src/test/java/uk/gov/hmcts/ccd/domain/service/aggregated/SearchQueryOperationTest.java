package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftAccessException;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.search.UseCase;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.processor.SearchInputProcessor;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway.DRAFT_STORE_DOWN_ERR_MESSAGE;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.NO_ERROR;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.SearchResultBuilder.searchResult;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildSearchResultField;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;

public class SearchQueryOperationTest {
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String EVENT_ID = "An event";
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_FIELD_ID_1_1 = "CASE_FIELD_1_1";
    private static final String CASE_FIELD_ID_1_2 = "CASE_FIELD_1_2";
    private static final String CASE_FIELD_ID_1_3 = "CASE_FIELD_1_3";
    private static final CaseFieldDefinition CASE_FIELD_1_1 = newCaseField().withId(CASE_FIELD_ID_1_1).build();
    private static final CaseFieldDefinition CASE_FIELD_1_2 = newCaseField().withId(CASE_FIELD_ID_1_2).build();
    private static final CaseFieldDefinition CASE_FIELD_1_3 = newCaseField().withId(CASE_FIELD_ID_1_3).build();
    private static final String DRAFT_ID = "1";
    private static final String CASE_FIELD_2 = "Case field 2";
    private static final String SEARCH_VIEW = "SEARCH";
    private static final String ASC = "ASC";
    private static final String DESC = "DESC";
    private static final String USER_ROLE_1 = "Role 1";
    private static final String USER_ROLE_2 = "Role 2";
    private static final String CASE_FIELD_PATH = "nestedFieldPath";

    @Mock
    private SearchOperation searchOperation;

    @Mock
    private MergeDataToSearchResultOperation mergeDataToSearchResultOperation;

    @Mock
    private GetCaseTypeOperation getCaseTypeOperation;

    @Mock
    private GetDraftsOperation getDraftsOperation;

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SearchInputProcessor searchInputProcessor;

    private SearchQueryOperation searchQueryOperation;
    private MetaData metadata;
    private HashMap<String, String> criteria;
    private List<CaseDetails> drafts = Lists.newArrayList();
    private List<CaseDetails> cases = Lists.newArrayList();
    private CaseTypeDefinition testCaseTypeDefinition;

    @Captor
    private ArgumentCaptor<List<CaseDetails>> argument;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        testCaseTypeDefinition = newCaseType()
            .withId(CASE_TYPE_ID)
            .withField(CASE_FIELD_1_1)
            .withField(CASE_FIELD_1_2)
            .withField(CASE_FIELD_1_3)
            .withEvent(newCaseEvent().withId(EVENT_ID).withCanSaveDraft(true).build())
            .build();
        Optional<CaseTypeDefinition> testCaseTypeOpt = Optional.of(testCaseTypeDefinition);

        doReturn(testCaseTypeOpt).when(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_READ);
        searchQueryOperation = new SearchQueryOperation(searchOperation,
                                                        mergeDataToSearchResultOperation,
                                                        getCaseTypeOperation,
                                                        getDraftsOperation, uiDefinitionRepository, userRepository,
                                                        searchInputProcessor);
        SearchResult searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""))
            .build();
        doReturn(searchResult).when(uiDefinitionRepository).getWorkBasketResult(CASE_TYPE_ID);
        doReturn(searchResult).when(uiDefinitionRepository).getSearchResult(CASE_TYPE_ID);
        doAnswer(i -> i.getArgument(2)).when(searchInputProcessor).executeQueryParams(Mockito.any(), Mockito.any(), Mockito.any());
        doAnswer(i -> i.getArgument(1)).when(searchInputProcessor).executeMetadata(Mockito.any(), Mockito.any());

        metadata = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        criteria = new HashMap<>();
        drafts.add(newCaseDetails()
                       .withId(DRAFT_ID)
                       .withJurisdiction(JURISDICTION_ID)
                       .withCaseTypeId(CASE_TYPE_ID)
                       .build());
        cases.add(newCaseDetails().build());
    }

    @Test
    @DisplayName("should search using search operation")
    public void shouldSearchUsingSearchOperation() {
        searchQueryOperation.execute(WORKBASKET, metadata, criteria);

        assertAll(
            () -> verify(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verify(getDraftsOperation).execute(metadata),
            () -> verify(mergeDataToSearchResultOperation).execute(any(), any(), anyList(), any())
        );
    }

    @Test
    @DisplayName("should include drafts if drafts enabled and if drafts are present")
    public void shouldIncludeDrafts() {
        doReturn(drafts).when(getDraftsOperation).execute(metadata);
        doReturn(cases).when(searchOperation).execute(metadata, criteria);

        searchQueryOperation.execute(WORKBASKET, metadata, criteria);

        assertAll(
            () -> verify(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verify(getDraftsOperation).execute(metadata),
            () -> verify(mergeDataToSearchResultOperation).execute(anyObject(), any(), argument.capture(), eq(NO_ERROR)),
            () -> assertThat(argument.getValue(), hasSize(2)),
            () -> assertThat(argument.getValue(), hasDraftItemInResults())
        );
    }

    @Test
    @DisplayName("should not call draft-store if drafts are not enabled")
    public void shouldNotCallDraftStore() {
        testCaseTypeDefinition.getEvents().get(0).setCanSaveDraft(false);
        doReturn(drafts).when(getDraftsOperation).execute(metadata);
        doReturn(cases).when(searchOperation).execute(metadata, criteria);

        searchQueryOperation.execute(WORKBASKET, metadata, criteria);

        assertAll(
            () -> verify(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verifyNoMoreInteractions(getDraftsOperation),
            () -> verify(mergeDataToSearchResultOperation).execute(anyObject(), any(), argument.capture(), eq(NO_ERROR)),
            () -> assertThat(argument.getValue(), hasSize(1)),
            () -> assertThat(argument.getValue(), not(hasDraftItemInResults()))
        );
    }

    @Test
    @DisplayName("should not call draft-store for actual Search")
    public void shouldNotCallDraftStoreForSearch() {
        doReturn(drafts).when(getDraftsOperation).execute(metadata);
        doReturn(cases).when(searchOperation).execute(metadata, criteria);

        searchQueryOperation.execute(SEARCH_VIEW, metadata, criteria);

        assertAll(
            () -> verify(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verifyNoMoreInteractions(getDraftsOperation),
            () -> verify(mergeDataToSearchResultOperation).execute(anyObject(), any(), argument.capture(), eq(NO_ERROR)),
            () -> assertThat(argument.getValue(), hasSize(1)),
            () -> assertThat(argument.getValue(), not(hasDraftItemInResults()))
        );
    }

    @Test
    @DisplayName("should return cases and resultError but not drafts when draft store unresponsive")
    public void shouldReturnCasesAndResultErrorButNoDraftsWhenDraftStoreUnresponsive() {
        DraftAccessException draftAccessException = new DraftAccessException(DRAFT_STORE_DOWN_ERR_MESSAGE);
        doThrow(draftAccessException).when(getDraftsOperation).execute(metadata);
        doReturn(cases).when(searchOperation).execute(metadata, criteria);
        searchQueryOperation.execute(WORKBASKET, metadata, criteria);

        assertAll(
            () -> verify(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verify(getDraftsOperation).execute(metadata),
            () -> verify(mergeDataToSearchResultOperation).execute(anyObject(), any(), eq(cases), eq(DRAFT_STORE_DOWN_ERR_MESSAGE))
        );
    }

    @Test
    @DisplayName("should return empty when caseType is not found")
    public void shouldReturnEmptyNoCaseTypeFound() {
        doReturn(Optional.empty()).when(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_READ);

        searchQueryOperation.execute(WORKBASKET, metadata, criteria);
        assertAll(
            () -> verify(getCaseTypeOperation).execute(CASE_TYPE_ID, CAN_READ),
            () -> verify(searchOperation, never()).execute(metadata, criteria),
            () -> verify(getDraftsOperation, never()).execute(metadata),
            () -> verify(mergeDataToSearchResultOperation, never()).execute(anyObject(), any(), anyList(), anyString())
        );
    }

    @Test
    @DisplayName("should get workBasketResult and pass to mergeDataToSearchResultOperation")
    public void shouldGetWorkBasketResultAndMerge() {
        SearchResult searchResult = searchResult()
             .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""))
             .build();
        doReturn(searchResult).when(uiDefinitionRepository).getWorkBasketResult(CASE_TYPE_ID);

        searchQueryOperation.execute(WORKBASKET, metadata, criteria);

        assertAll(
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verify(mergeDataToSearchResultOperation).execute(any(), eq(searchResult), anyList(), any())
        );
    }

    @Test
    @DisplayName("should get searchResult and pass to mergeDataToSearchResultOperation")
    public void shouldGetSearchResultsAndMerge() {
        SearchResult searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, ""))
            .build();
        doReturn(searchResult).when(uiDefinitionRepository).getSearchResult(CASE_TYPE_ID);

        searchQueryOperation.execute(SEARCH_VIEW, metadata, criteria);

        assertAll(
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verify(mergeDataToSearchResultOperation).execute(any(), eq(searchResult), anyList(), any())
        );
    }

    @Test
    @DisplayName("should build sortOrderFields from sort search results fields only")
    public void shouldBuildSortOrderFieldsFromSortResultsFieldsOnly() {
        SearchResultField nonSortField = buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_2, "", CASE_FIELD_2, "");
        SearchResultField sortField = buildSortResultField(CASE_FIELD_2, "", null, ASC, 1);
        SearchResult searchResult = searchResult()
            .withSearchResultFields(nonSortField, sortField)
            .build();

        doReturn(searchResult).when(uiDefinitionRepository).getWorkBasketResult(CASE_TYPE_ID);

        searchQueryOperation.execute(WORKBASKET, metadata, criteria);

        assertThat(metadata.getSortOrderFields().size(), is(1));
        assertThat(metadata.getSortOrderFields().get(0).getCaseFieldId(), is(CASE_FIELD_2));
        assertThat(metadata.getSortOrderFields().get(0).isMetadata(), is(false));
        assertThat(metadata.getSortOrderFields().get(0).getDirection(), is(ASC));
    }

    @Test
    @DisplayName("should build sortOrderFields based on priority order")
    public void shouldBuildSortOrderFieldsInTheOrderOfPriority() {
        SearchResultField sortField1 = buildSortResultField(CASE_FIELD_ID_1_1, "", null, ASC, 2);
        SearchResultField sortField2 = buildSortResultField(CASE_FIELD_ID_1_2, "", null, DESC, 1);
        SearchResult searchResult = searchResult()
            .withSearchResultFields(sortField1, sortField2)
            .build();

        doReturn(searchResult).when(uiDefinitionRepository).getWorkBasketResult(CASE_TYPE_ID);

        searchQueryOperation.execute(WORKBASKET, metadata, criteria);

        assertThat(metadata.getSortOrderFields().size(), is(2));
        assertThat(metadata.getSortOrderFields().get(0).getCaseFieldId(), is(CASE_FIELD_ID_1_2));
        assertThat(metadata.getSortOrderFields().get(0).getDirection(), is(DESC));
        assertThat(metadata.getSortOrderFields().get(1).getCaseFieldId(), is(CASE_FIELD_ID_1_1));
        assertThat(metadata.getSortOrderFields().get(1).getDirection(), is(ASC));
    }

    @Test
    @DisplayName("should build sortOrderFields based on user role")
    public void shouldFilterSortOrderFieldsBasedOnUserRole() {
        SearchResultField sortField1 = buildSortResultField(CASE_FIELD_ID_1_1, "", USER_ROLE_1, ASC, 2);
        SearchResultField sortField2 = buildSortResultField(CASE_FIELD_ID_1_2, CASE_FIELD_PATH, USER_ROLE_2, DESC, 1);
        SearchResult searchResult = searchResult()
            .withSearchResultFields(sortField1, sortField2)
            .build();

        doReturn(searchResult).when(uiDefinitionRepository).getWorkBasketResult(CASE_TYPE_ID);
        doReturn(Sets.newHashSet(USER_ROLE_2)).when(userRepository).getUserRoles();

        searchQueryOperation.execute(WORKBASKET, metadata, criteria);

        assertThat(metadata.getSortOrderFields().size(), is(1));
        assertThat(metadata.getSortOrderFields().get(0).getCaseFieldId(), is(CASE_FIELD_ID_1_2 + "." + CASE_FIELD_PATH));
        assertThat(metadata.getSortOrderFields().get(0).getDirection(), is(DESC));
    }

    @Test
    public void shouldGetSearchResultDefinitionForWorkbasket() {
        SearchResult searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_ID_1_1, "", CASE_FIELD_ID_1_1, ""))
            .build();
        doReturn(searchResult).when(uiDefinitionRepository).getWorkBasketResult(CASE_TYPE_ID);

        final SearchResult result = searchQueryOperation.getSearchResultDefinition(testCaseTypeDefinition, UseCase.WORKBASKET);

        verify(uiDefinitionRepository).getWorkBasketResult(eq(CASE_TYPE_ID));
        verifyNoMoreInteractions(uiDefinitionRepository);
        assertAll(
            () -> assertThat(result, is(searchResult))
        );
    }

    @Test
    public void shouldGetSearchResultDefinitionForSearch() {
        SearchResult searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_ID_1_1, "", CASE_FIELD_ID_1_1, ""))
            .build();
        doReturn(searchResult).when(uiDefinitionRepository).getSearchResult(CASE_TYPE_ID);

        final SearchResult result = searchQueryOperation.getSearchResultDefinition(testCaseTypeDefinition, UseCase.SEARCH);

        verify(uiDefinitionRepository).getSearchResult(eq(CASE_TYPE_ID));
        verifyNoMoreInteractions(uiDefinitionRepository);
        assertAll(
            () -> assertThat(result, is(searchResult))
        );
    }

    @Test
    public void shouldGetSearchResultDefinitionForOrgCases() {
        SearchResult searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_ID_1_1, "", CASE_FIELD_ID_1_1, ""))
            .build();
        doReturn(searchResult).when(uiDefinitionRepository).getSearchCasesResult(CASE_TYPE_ID, UseCase.ORG_CASES);

        final SearchResult result = searchQueryOperation.getSearchResultDefinition(testCaseTypeDefinition, UseCase.ORG_CASES);

        verify(uiDefinitionRepository).getSearchCasesResult(eq(CASE_TYPE_ID), eq(UseCase.ORG_CASES));
        verifyNoMoreInteractions(uiDefinitionRepository);
        assertAll(
            () -> assertThat(result, is(searchResult))
        );
    }

    @Test
    public void shouldGetSearchResultDefinitionForDefaultUseCase() {
        CaseFieldDefinition caseField = newCaseField()
            .withId(CASE_FIELD_ID_1_1)
            .withCaseTypeId(CASE_TYPE_ID)
            .withFieldLabelText("Label")
            .withMetadata(true)
            .build();
        CaseTypeDefinition caseTypeDefinition = newCaseType()
            .withId(CASE_TYPE_ID)
            .withField(caseField)
            .build();

        final SearchResult result = searchQueryOperation.getSearchResultDefinition(caseTypeDefinition, UseCase.DEFAULT);

        verifyNoMoreInteractions(uiDefinitionRepository);
        assertAll(
            () -> assertThat(result.getFields().length, is(1)),
            () -> assertThat(result.getFields()[0].getCaseFieldId(), is(CASE_FIELD_ID_1_1)),
            () -> assertThat(result.getFields()[0].getCaseTypeId(), is(CASE_TYPE_ID)),
            () -> assertThat(result.getFields()[0].getLabel(), is("Label")),
            () -> assertThat(result.getFields()[0].isMetadata(), is(true))
        );
    }

    private static SearchResultField buildSortResultField(String caseFieldId,
                                                    String caseFieldPath,
                                                    String role,
                                                    String sortDirection, Integer sortPriority) {
        SortOrder sortOrder = getSortOrder(sortDirection, sortPriority);
        SearchResultField searchResultField = buildSearchResultField(CASE_TYPE_ID, caseFieldId, caseFieldPath, caseFieldId, "");
        searchResultField.setSortOrder(sortOrder);
        searchResultField.setRole(role);
        return searchResultField;
    }

    private static SortOrder getSortOrder(String sortDirection, Integer sortPriority) {
        SortOrder sortOrder = new SortOrder();
        sortOrder.setDirection(sortDirection);
        sortOrder.setPriority(sortPriority);
        return sortOrder;
    }

    private Matcher<Iterable<? super CaseDetails>> hasDraftItemInResults() {
        return hasItem(allOf(hasProperty("id", is(DRAFT_ID)),
                             hasProperty("jurisdiction", is(JURISDICTION_ID)),
                             hasProperty("caseTypeId", is(CASE_TYPE_ID))));
    }

}
