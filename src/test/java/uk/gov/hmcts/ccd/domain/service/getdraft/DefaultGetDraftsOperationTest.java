package uk.gov.hmcts.ccd.domain.service.getdraft;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.DraftResponseToCaseDetailsBuilder;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDraftBuilder.newCaseDraft;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.newDraftResponse;

class DefaultGetDraftsOperationTest {

    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String OTHER_CASE_TYPE_ID = "PROBATE";
    private static final String JURISDICTION_ID = "Probate";
    private static final String OTHER_JURISDICTION_ID = "DIVORCE";
    private static final String DRAFT_ID = "1";
    private static final CaseDataContent CASE_DATA_CONTENT = new CaseDataContent();

    private List<DraftResponse> drafts = Lists.newArrayList();
    private DraftResponse draft = new DraftResponse();
    private List<CaseDetails> cases = Lists.newArrayList();
    private CaseDetails caseDetails;
    private MetaData metadata;

    @Mock
    private DraftGateway draftGateway;
    @Mock
    private DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;

    private GetDraftsOperation getDraftsOperation;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        metadata = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        draft = newDraftResponse()
            .withId(DRAFT_ID)
            .withDocument(newCaseDraft()
                              .withJurisdictionId(JURISDICTION_ID)
                              .withCaseTypeId(CASE_TYPE_ID)
                              .withCaseDataContent(CASE_DATA_CONTENT)
                              .build())
            .build();
        drafts.add(draft);
        doReturn(drafts).when(draftGateway).getAll();
        caseDetails = newCaseDetails()
            .withCaseTypeId(CASE_TYPE_ID)
            .withJurisdiction(JURISDICTION_ID)
            .withId(DRAFT_ID)
            .withData(CASE_DATA_CONTENT.getData())
            .build();
        cases.add(caseDetails);

        getDraftsOperation = new DefaultGetDraftsOperation(draftGateway, draftResponseToCaseDetailsBuilder);
    }

    @Test
    @DisplayName("should not return drafts when no page param in metadata")
    public void shouldNotReturnDraftsWhenNoPageParamInMetadata() {

        List<CaseDetails> draftCases = getDraftsOperation.execute(metadata);

        assertAll(
            () -> verify(draftGateway, never()).getAll(),
            () -> assertThat(draftCases, hasSize(0))
        );
    }

    @Test
    @DisplayName("should not return drafts when not on first page")
    public void shouldNotReturnDraftsWhenNotOnFirstPage() {
        metadata.setPage(Optional.of("2"));

        List<CaseDetails> draftCases = getDraftsOperation.execute(metadata);

        assertAll(
            () -> verify(draftGateway, never()).getAll(),
            () -> assertThat(draftCases, hasSize(0))
        );
    }

    @Test
    @DisplayName("should return drafts matching criteria on first page")
    public void shouldReturnDraftsWhenOnFirstPage() {
        metadata.setPage(Optional.of("1"));
        doReturn(caseDetails).when(draftResponseToCaseDetailsBuilder).build(draft);

        List<CaseDetails> draftCases = getDraftsOperation.execute(metadata);

        assertAll(
            () -> verify(draftGateway).getAll(),
            () -> assertThat(draftCases, hasSize(1)),
            () -> assertThat(draftCases, hasDraftItemInResults())
        );
    }

    @Test
    @DisplayName("should not return drafts that do not match jurisdiction criteria")
    public void shouldNotReturnDraftsThatDoNotMatchJurisdiction() {
        drafts = Lists.newArrayList();
        drafts.add(newDraftResponse()
                       .withId(DRAFT_ID)
                       .withDocument(newCaseDraft()
                                         .withJurisdictionId(OTHER_JURISDICTION_ID)
                                         .withCaseTypeId(CASE_TYPE_ID)
                                         .withCaseDataContent(CASE_DATA_CONTENT)
                                         .build())
                       .build());
        doReturn(drafts).when(draftGateway).getAll();
        metadata.setPage(Optional.of("1"));

        List<CaseDetails> draftCases = getDraftsOperation.execute(metadata);

        assertAll(
            () -> verify(draftGateway).getAll(),
            () -> assertThat(draftCases, hasSize(0))
        );
    }

    @Test
    @DisplayName("should not return drafts that do not match case type criteria")
    public void shouldNotReturnDraftsThatDoNotMatchCaseType() {
        drafts = Lists.newArrayList();
        drafts.add(newDraftResponse()
                       .withId(DRAFT_ID)
                       .withDocument(newCaseDraft()
                                         .withJurisdictionId(JURISDICTION_ID)
                                         .withCaseTypeId(OTHER_CASE_TYPE_ID)
                                         .withCaseDataContent(CASE_DATA_CONTENT)
                                         .build())
                       .build());
        doReturn(drafts).when(draftGateway).getAll();
        metadata.setPage(Optional.of("1"));

        List<CaseDetails> draftCases = getDraftsOperation.execute(metadata);

        assertAll(
            () -> verify(draftGateway).getAll(),
            () -> assertThat(draftCases, hasSize(0))
        );
    }

    private Matcher<Iterable<? super CaseDetails>> hasDraftItemInResults() {
        return hasItem(allOf(hasProperty("id", is(DRAFT_ID)),
                             hasProperty("jurisdiction", is(JURISDICTION_ID)),
                             hasProperty("caseTypeId", is(CASE_TYPE_ID)),
                             hasProperty("data", is(CASE_DATA_CONTENT.getData()))));
    }
}
