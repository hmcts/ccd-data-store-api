package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.draft.DraftAccessException;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway.DRAFT_STORE_DOWN_ERR_MESSAGE;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.NO_ERROR;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.anCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.aCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;

public class SearchQueryOperationTest {
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String EVENT_ID = "An event";
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_FIELD_ID_1_1 = "CASE_FIELD_1_1";
    private static final String CASE_FIELD_ID_1_2 = "CASE_FIELD_1_2";
    private static final String CASE_FIELD_ID_1_3 = "CASE_FIELD_1_3";
    private static final CaseField CASE_FIELD_1_1 = aCaseField().withId(CASE_FIELD_ID_1_1).build();
    private static final CaseField CASE_FIELD_1_2 = aCaseField().withId(CASE_FIELD_ID_1_2).build();
    private static final CaseField CASE_FIELD_1_3 = aCaseField().withId(CASE_FIELD_ID_1_3).build();
    private static final String DRAFT_ID = "1";

    @Mock
    private SearchOperation searchOperation;

    @Mock
    private MergeDataToSearchResultOperation mergeDataToSearchResultOperation;

    @Mock
    private GetCaseTypesOperation getCaseTypesOperation;

    @Mock
    private GetDraftsOperation getDraftsOperation;

    private SearchQueryOperation searchQueryOperation;
    private MetaData metadata;
    private HashMap<String, String> criteria;
    private List<CaseDetails> drafts = Lists.newArrayList();
    private List<CaseDetails> cases = Lists.newArrayList();
    private CaseType testCaseType;

    @Mock
    private CaseTypeService caseTypeService;

    @Captor
    private ArgumentCaptor<List<CaseDetails>> argument;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        testCaseType = newCaseType()
            .withId(CASE_TYPE_ID)
            .withField(CASE_FIELD_1_1)
            .withField(CASE_FIELD_1_2)
            .withField(CASE_FIELD_1_3)
            .withEvent(anCaseEvent().withId(EVENT_ID).withCanSaveDraft(true).build())
            .build();
        List<CaseType> testCaseTypes = Lists.newArrayList(testCaseType);

        doReturn(testCaseTypes).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ);
        doReturn(testCaseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        searchQueryOperation = new SearchQueryOperation(searchOperation,
                                                        mergeDataToSearchResultOperation,
                                                        getCaseTypesOperation,
                                                        getDraftsOperation,
                                                        caseTypeService);
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
            () -> verify(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verify(getDraftsOperation).execute(metadata),
            () -> verify(mergeDataToSearchResultOperation).execute(anyObject(), anyList(), anyString(), anyString())
        );
    }

    @Test
    @DisplayName("should include drafts if drafts enabled and if drafts are present")
    public void shouldIncludeDrafts() {
        doReturn(drafts).when(getDraftsOperation).execute(metadata);
        doReturn(cases).when(searchOperation).execute(metadata, criteria);

        searchQueryOperation.execute(WORKBASKET, metadata, criteria);

        assertAll(
            () -> verify(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verify(getDraftsOperation).execute(metadata),
            () -> verify(mergeDataToSearchResultOperation).execute(anyObject(), argument.capture(), anyString(), eq(NO_ERROR)),
            () -> assertThat(argument.getValue(), hasSize(2)),
            () -> assertThat(argument.getValue(), hasDraftItemInResults())
        );
    }

    @Test
    @DisplayName("should not call draft-store if drafts are not enabled")
    public void shouldNotCallDraftStore() {
        testCaseType.getEvents().get(0).setCanSaveDraft(false);
        doReturn(drafts).when(getDraftsOperation).execute(metadata);
        doReturn(cases).when(searchOperation).execute(metadata, criteria);

        searchQueryOperation.execute(WORKBASKET, metadata, criteria);

        assertAll(
            () -> verify(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verifyNoMoreInteractions(getDraftsOperation),
            () -> verify(mergeDataToSearchResultOperation).execute(anyObject(), argument.capture(), anyString(), eq(NO_ERROR)),
            () -> assertThat(argument.getValue(), hasSize(1)),
            () -> assertThat(argument.getValue(), not(hasDraftItemInResults()))
        );
    }

    @Test
    @DisplayName("should not call draft-store for actual Search")
    public void shouldNotCallDraftStoreForSearch() {
        doReturn(drafts).when(getDraftsOperation).execute(metadata);
        doReturn(cases).when(searchOperation).execute(metadata, criteria);

        searchQueryOperation.execute("not a" + WORKBASKET, metadata, criteria);

        assertAll(
            () -> verify(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verifyNoMoreInteractions(getDraftsOperation),
            () -> verify(mergeDataToSearchResultOperation).execute(anyObject(), argument.capture(), anyString(), eq(NO_ERROR)),
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
            () -> verify(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verify(getDraftsOperation).execute(metadata),
            () -> verify(mergeDataToSearchResultOperation).execute(anyObject(), eq(cases), anyString(), eq(DRAFT_STORE_DOWN_ERR_MESSAGE))
        );
    }

    @Test
    @DisplayName("should return empty when caseType is not found")
    public void shouldReturnEmptyNoCaseTypeFound() {
        doReturn(new ArrayList<>()).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ);

        searchQueryOperation.execute(WORKBASKET, metadata, criteria);
        assertAll(
            () -> verify(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID),
            () -> verify(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ),
            () -> verify(searchOperation, never()).execute(metadata, criteria),
            () -> verify(getDraftsOperation, never()).execute(metadata),
            () -> verify(mergeDataToSearchResultOperation, never()).execute(anyObject(), anyList(), anyString(), anyString())
        );
    }

    private Matcher<Iterable<? super CaseDetails>> hasDraftItemInResults() {
        return hasItem(allOf(hasProperty("id", is(DRAFT_ID)),
                             hasProperty("jurisdiction", is(JURISDICTION_ID)),
                             hasProperty("caseTypeId", is(CASE_TYPE_ID))));
    }

}
