package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.upsertdraft.UpsertDraftOperation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.anCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.anDraftResponse;

class DraftsEndpointTest {

    private static final String UID = "1231";
    private static final String JURISDICTION_ID = "Test";
    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final CaseDataContent CASE_DATA_CONTENT = anCaseDataContent().build();
    private static final String EVENT_TRIGGER_ID = "createCase";
    private static final String DRAFT_ID = "5";

    @Mock
    private UpsertDraftOperation upsertDraftOperation;

    private DraftsEndpoint endpoint;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        endpoint = new DraftsEndpoint(upsertDraftOperation);
    }

    @Test
    void shouldSaveDraftForCaseWorker() {
        final DraftResponse toBeReturned = anDraftResponse().build();
        doReturn(toBeReturned).when(upsertDraftOperation).executeSave(UID,
                                                                      JURISDICTION_ID,
                                                                      CASE_TYPE_ID,
                                                                      EVENT_TRIGGER_ID,
                                                                      CASE_DATA_CONTENT);

        final DraftResponse output = endpoint.saveDraftForCaseWorker(UID,
                                                                     JURISDICTION_ID,
                                                                     CASE_TYPE_ID,
                                                                     EVENT_TRIGGER_ID,
                                                                     CASE_DATA_CONTENT);

        assertAll(
            () -> assertThat(output, sameInstance(toBeReturned)),
            () -> verify(upsertDraftOperation).executeSave(UID,
                                                           JURISDICTION_ID,
                                                           CASE_TYPE_ID,
                                                           EVENT_TRIGGER_ID,
                                                           CASE_DATA_CONTENT)
        );
    }

    @Test
    void shouldUpdateDraftForCaseWorker() {
        final DraftResponse toBeReturned = anDraftResponse().build();
        doReturn(toBeReturned).when(upsertDraftOperation).executeUpdate(UID,
                                                                        JURISDICTION_ID,
                                                                        CASE_TYPE_ID,
                                                                        EVENT_TRIGGER_ID,
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
            () -> verify(upsertDraftOperation).executeUpdate(UID,
                                                             JURISDICTION_ID,
                                                             CASE_TYPE_ID,
                                                             EVENT_TRIGGER_ID,
                                                             DRAFT_ID,
                                                             CASE_DATA_CONTENT)
        );
    }

}
