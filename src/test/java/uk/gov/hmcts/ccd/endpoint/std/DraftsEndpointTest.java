package uk.gov.hmcts.ccd.endpoint.std;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.newDraftResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.upsertdraft.UpsertDraftOperation;

class DraftsEndpointTest {

    private static final String UID = "1231";
    private static final String JURISDICTION_ID = "Test";
    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final CaseDataContent CASE_DATA_CONTENT = newCaseDataContent().build();
    private static final String EVENT_TRIGGER_ID = "createCase";
    private static final String DRAFT_ID = "4444333322221111";

    @Mock
    private UpsertDraftOperation upsertDraftOperation;
    @Mock
    private GetCaseViewOperation getDraftViewOperation;
    @Mock
    private DraftGateway draftGateway;

    private DraftsEndpoint endpoint;

    @Mock
    private UIDService uidService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        endpoint = new DraftsEndpoint(upsertDraftOperation, getDraftViewOperation, draftGateway, uidService);
    }

    @Test
    void shouldSaveDraftForCaseWorker() {
        final DraftResponse toBeReturned = newDraftResponse().build();
        doReturn(toBeReturned).when(upsertDraftOperation).executeSave(CASE_TYPE_ID,
                                                                      CASE_DATA_CONTENT);

        final DraftResponse output = endpoint.saveDraftForCaseWorker(UID,
                                                                     JURISDICTION_ID,
                                                                     CASE_TYPE_ID,
                                                                     EVENT_TRIGGER_ID,
                                                                     CASE_DATA_CONTENT);

        assertAll(
            () -> assertThat(output, sameInstance(toBeReturned)),
            () -> verify(upsertDraftOperation).executeSave(CASE_TYPE_ID,
                                                           CASE_DATA_CONTENT)
        );
    }

    @Test
    void shouldUpdateDraftForCaseWorker() {
        when(uidService.validateUID(anyString())).thenReturn(true);
        final DraftResponse toBeReturned = newDraftResponse().build();
        doReturn(toBeReturned).when(upsertDraftOperation).executeUpdate(CASE_TYPE_ID,
                                                                        DRAFT_ID,
                                                                        CASE_DATA_CONTENT);

        final DraftResponse output = endpoint.updateDraftForCaseWorker(UID,
                                                                       JURISDICTION_ID,
                                                                       CASE_TYPE_ID,
                                                                       EVENT_TRIGGER_ID,
                                                                       DRAFT_ID,
                                                                       CASE_DATA_CONTENT);

        assertAll(
            () -> assertThat(output, sameInstance(toBeReturned)),
            () -> verify(upsertDraftOperation).executeUpdate(CASE_TYPE_ID,
                                                             DRAFT_ID,
                                                             CASE_DATA_CONTENT)
        );
    }

    @Test
    void shouldFetchDraftForCaseWorker() {
        CaseView toBeReturned = new CaseView();
        doReturn(toBeReturned).when(getDraftViewOperation).execute(any());
        final CaseView output = endpoint.findDraft(UID, JURISDICTION_ID, CASE_TYPE_ID, DRAFT_ID);

        assertAll(
            () -> assertThat(output, sameInstance(toBeReturned)),
            () -> verify(getDraftViewOperation).execute(DRAFT_ID)
        );
    }


    @Test
    void shouldDeleteDraftForCaseWorker() {
        when(uidService.validateUID(anyString())).thenReturn(true);
        doNothing().when(draftGateway).delete(DRAFT_ID);

        endpoint.deleteDraft(UID, JURISDICTION_ID, CASE_TYPE_ID, DRAFT_ID);

        assertAll(
            () -> verify(draftGateway).delete(DRAFT_ID)
        );
    }

}
