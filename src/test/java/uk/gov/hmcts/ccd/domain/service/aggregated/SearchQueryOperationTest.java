package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.draft.DraftAccessException;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway.DRAFT_ACCESS_EXCEPTION_MSG;
import static uk.gov.hmcts.ccd.domain.model.definition.CaseDetailsBuilder.aCaseDetails;
import static uk.gov.hmcts.ccd.domain.model.draft.CaseDraftBuilder.aCaseDraft;
import static uk.gov.hmcts.ccd.domain.model.draft.DraftResponseBuilder.aDraftResponse;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.NO_ERROR;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.aCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.aCaseType;

public class SearchQueryOperationTest {

    private static final String VIEW = "WORKBASKET";
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_FIELD_ID_1_1 = "CASE_FIELD_1_1";
    private static final String CASE_FIELD_ID_1_2 = "CASE_FIELD_1_2";
    private static final String CASE_FIELD_ID_1_3 = "CASE_FIELD_1_3";
    private static final CaseField CASE_FIELD_1_1 = aCaseField().withId(CASE_FIELD_ID_1_1).build();
    private static final CaseField CASE_FIELD_1_2 = aCaseField().withId(CASE_FIELD_ID_1_2).build();
    private static final CaseField CASE_FIELD_1_3 = aCaseField().withId(CASE_FIELD_ID_1_3).build();
    private static final String DRAFT_ID = "1";
    private static final CaseDataContent CASE_DATA_CONTENT = new CaseDataContent();

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
    private List<DraftResponse> drafts = Lists.newArrayList();

    @Mock
    private CaseTypeService caseTypeService;
    private List<CaseDetails> cases = Lists.newArrayList();

    @Captor
    private ArgumentCaptor<List<CaseDetails>> argument;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        CaseType testCaseType = aCaseType()
            .withField(CASE_FIELD_1_1)
            .withField(CASE_FIELD_1_2)
            .withField(CASE_FIELD_1_3)
            .build();
        testCaseType.setId(CASE_TYPE_ID);
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
        drafts.add(aDraftResponse()
                       .withId(DRAFT_ID)
                       .withDocument(aCaseDraft()
                                         .withJurisdictionId(JURISDICTION_ID)
                                         .withCaseTypeId(CASE_TYPE_ID)
                                         .withCaseDataContent(CASE_DATA_CONTENT)
                                         .build())
                       .build());
        cases.add(aCaseDetails().build());
    }

    @Test
    @DisplayName("should search using search operation")
    public void shouldSearchUsingSearchOperation() {
        searchQueryOperation.execute(VIEW, metadata, criteria);

        assertAll(
            () -> verify(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verify(getDraftsOperation, never()).execute(),
            () -> verify(mergeDataToSearchResultOperation, times(1)).execute(anyObject(), anyList(), anyString(), anyString())
        );
    }

    @Test
    @DisplayName("should include drafts on first page")
    public void shouldIncludeDraftsWhenOnFirstPage() {
        doReturn(drafts).when(getDraftsOperation).execute();
        metadata.setPage(Optional.of("1"));
        doReturn(cases).when(searchOperation).execute(metadata, criteria);

        searchQueryOperation.execute(VIEW, metadata, criteria);

        assertAll(
            () -> verify(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verify(getDraftsOperation).execute(),
            () -> verify(mergeDataToSearchResultOperation).execute(anyObject(), argument.capture(), anyString(), eq(NO_ERROR)),
            () -> assertThat(argument.getValue().size(), is(2)),
            () -> assertThat(argument.getValue(), hasDraftItemInResults())
        );
    }

    @Test
    @DisplayName("should return cases and resultError but not drafts when draft store unresponsive")
    public void shouldReturnCasesAndResultErrorButNoDraftsWhenDraftStoreUnresponsive() {
        DraftAccessException draftAccessException = new DraftAccessException(DRAFT_ACCESS_EXCEPTION_MSG);
        doThrow(draftAccessException).when(getDraftsOperation).execute();
        metadata.setPage(Optional.of("1"));
        doReturn(cases).when(searchOperation).execute(metadata, criteria);
        searchQueryOperation.execute(VIEW, metadata, criteria);

        assertAll(
            () -> verify(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ),
            () -> verify(searchOperation).execute(metadata, criteria),
            () -> verify(getDraftsOperation).execute(),
            () -> verify(mergeDataToSearchResultOperation).execute(anyObject(), eq(cases), anyString(), eq(DRAFT_ACCESS_EXCEPTION_MSG))
        );
    }

    @Test
    @DisplayName("should return empty when caseType is not found")
    public void shouldReturnEmptyNoCaseTypeFound() {
        doReturn(new ArrayList<>()).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ);

        searchQueryOperation.execute(VIEW, metadata, criteria);
        assertAll(
            () -> verify(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID),
            () -> verify(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ),
            () -> verify(searchOperation, never()).execute(metadata, criteria),
            () -> verify(getDraftsOperation, never()).execute(),
            () -> verify(mergeDataToSearchResultOperation, never()).execute(anyObject(), anyList(), anyString(), anyString())
        );
    }

    private Matcher<Iterable<? super CaseDetails>> hasDraftItemInResults() {
        return hasItem(allOf(hasProperty("id", is(Long.valueOf(DRAFT_ID))),
                             hasProperty("jurisdiction", is(JURISDICTION_ID)),
                             hasProperty("caseTypeId", is(CASE_TYPE_ID))));
    }

}
