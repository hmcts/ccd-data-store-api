package uk.gov.hmcts.ccd.domain.service.upsertdraft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.anCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.anDraftResponse;
import static uk.gov.hmcts.ccd.domain.service.upsertdraft.DefaultUpsertDraftOperation.CASE_DATA_CONTENT;

class DefaultUpsertDraftOperationTest {

    public static final int DRAFT_MAX_STALE_DAYS = 7;
    private static final String UID = "1";
    private static final String JID = "TEST";
    private static final String CTID = "TestAddressBookCase";
    private static final String ETID = "createCase";
    private static final String DID = "5";
    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private DraftGateway draftGateway;

    private UpsertDraftOperation upsertDraftOperation;

    private CaseDataContent caseDataContent = anCaseDataContent().build();
    private CaseDraft caseDraft;
    private DraftResponse draftResponse = anDraftResponse().build();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(applicationParams.getDraftMaxTTLDays()).thenReturn(DRAFT_MAX_STALE_DAYS);

        upsertDraftOperation = new DefaultUpsertDraftOperation(draftGateway, applicationParams);
        caseDraft = new CaseDraft();
        caseDraft.setUserId(UID);
        caseDraft.setJurisdictionId(JID);
        caseDraft.setCaseTypeId(CTID);
        caseDraft.setEventTriggerId(ETID);
        caseDraft.setCaseDataContent(caseDataContent);
    }

    @Test
    void shouldSuccessfullySaveDraft() {
        final ArgumentCaptor<CreateCaseDraftRequest> argument = ArgumentCaptor.forClass(CreateCaseDraftRequest.class);
        doReturn(Long.valueOf(DID)).when(draftGateway).save(any(CreateCaseDraftRequest.class));
        draftResponse.setId(DID);

        DraftResponse result = upsertDraftOperation.executeSave(UID, JID, CTID, ETID, caseDataContent);

        assertAll(
            () ->  verify(draftGateway).save(argument.capture()),
            () ->  assertThat(argument.getValue().getDocument(), hasProperty("userId", is(caseDraft.getUserId()))),
            () ->  assertThat(argument.getValue().getDocument(), hasProperty("jurisdictionId", is(caseDraft.getJurisdictionId()))),
            () ->  assertThat(argument.getValue().getDocument(), hasProperty("caseTypeId", is(caseDraft.getCaseTypeId()))),
            () ->  assertThat(argument.getValue().getDocument(), hasProperty("eventTriggerId", is(caseDraft.getEventTriggerId()))),
            () ->  assertThat(argument.getValue().getDocument(), hasProperty("caseDataContent", is(caseDataContent))),
            () ->  assertThat(argument.getValue().getMaxTTLDays(), is(DRAFT_MAX_STALE_DAYS)),
            () ->  assertThat(argument.getValue().getType(), is(CASE_DATA_CONTENT)),
            () ->  assertThat(result, samePropertyValuesAs(draftResponse))
        );
    }

    @Test
    void shouldSuccessfullyUpdateDraft() {
        final ArgumentCaptor<String> draftIdCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<UpdateCaseDraftRequest> caseDataContentCaptor = ArgumentCaptor.forClass(UpdateCaseDraftRequest.class);
        doReturn(draftResponse).when(draftGateway).update(any(UpdateCaseDraftRequest.class), any(String.class));


        DraftResponse result = upsertDraftOperation.executeUpdate(UID, JID, CTID, ETID, DID, caseDataContent);

        assertAll(
            () ->  verify(draftGateway).update(caseDataContentCaptor.capture(), draftIdCaptor.capture()),
            () ->  assertThat(draftIdCaptor.getValue(), is("5")),
            () ->  assertThat(caseDataContentCaptor.getValue().getDocument(), hasProperty("userId", is(caseDraft.getUserId()))),
            () ->  assertThat(caseDataContentCaptor.getValue().getDocument(), hasProperty("jurisdictionId", is(caseDraft.getJurisdictionId()))),
            () ->  assertThat(caseDataContentCaptor.getValue().getDocument(), hasProperty("caseTypeId", is(caseDraft.getCaseTypeId()))),
            () ->  assertThat(caseDataContentCaptor.getValue().getDocument(), hasProperty("eventTriggerId", is(caseDraft.getEventTriggerId()))),
            () ->  assertThat(caseDataContentCaptor.getValue().getDocument(), hasProperty("caseDataContent", is(caseDataContent))),
            () ->  assertThat(caseDataContentCaptor.getValue().getType(), is(CASE_DATA_CONTENT)),
            () ->  assertThat(result, is(draftResponse))
        );
    }

}
