package uk.gov.hmcts.ccd.domain.service.createdraft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.draft.*;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import java.net.URISyntaxException;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.draft.CaseDraftBuilder.aCaseDraft;
import static uk.gov.hmcts.ccd.domain.service.createdraft.DefaultUpsertDraftOperation.CASE_DATA_CONTENT;

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

    private CaseDataContent caseDataContent = new CaseDataContent();
    private CaseDraft caseDraft;
    private Draft draft = new Draft();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(applicationParams.getDraftMaxStaleDays()).thenReturn(DRAFT_MAX_STALE_DAYS);

        upsertDraftOperation = new DefaultUpsertDraftOperation(draftGateway, applicationParams);
    }

    @Test
    void shouldSuccessfullySaveDraft() throws URISyntaxException {
        caseDraft = aCaseDraft()
            .withUserId(UID)
            .withJurisdictionId(JID)
            .withCaseTypeId(CTID)
            .withEventTriggerId(ETID)
            .withCaseDataContent(caseDataContent)
            .build();
        final ArgumentCaptor<CreateCaseDraft> argument = ArgumentCaptor.forClass(CreateCaseDraft.class);
        doReturn(draft).when(draftGateway).save(any(CreateCaseDraft.class));


        Draft result = upsertDraftOperation.saveDraft(UID, JID, CTID, ETID, caseDataContent);

        assertAll(
            () ->  verify(draftGateway).save(argument.capture()),
            () ->  assertThat(argument.getValue().document, hasProperty("userId", is(caseDraft.getUserId()))),
            () ->  assertThat(argument.getValue().document, hasProperty("jurisdictionId", is(caseDraft.getJurisdictionId()))),
            () ->  assertThat(argument.getValue().document, hasProperty("caseTypeId", is(caseDraft.getCaseTypeId()))),
            () ->  assertThat(argument.getValue().document, hasProperty("eventTriggerId", is(caseDraft.getEventTriggerId()))),
            () ->  assertThat(argument.getValue().document, hasProperty("caseDataContent", is(caseDataContent))),
            () ->  assertThat(argument.getValue().maxStaleDays, is(DRAFT_MAX_STALE_DAYS)),
            () ->  assertThat(argument.getValue().type, is(CASE_DATA_CONTENT)),
            () ->  assertThat(result, is(draft))
        );
    }

    @Test
    void shouldSuccessfullyUpdateDraft() throws URISyntaxException {
        caseDraft = new CaseDraftBuilder()
            .withUserId(UID)
            .withJurisdictionId(JID)
            .withCaseTypeId(CTID)
            .withEventTriggerId(ETID)
            .withCaseDataContent(caseDataContent)
            .build();
        final ArgumentCaptor<String> draftIdCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<UpdateCaseDraft> caseDataContentCaptor = ArgumentCaptor.forClass(UpdateCaseDraft.class);
        doReturn(draft).when(draftGateway).update(any(UpdateCaseDraft.class), any(String.class));


        Draft result = upsertDraftOperation.updateDraft(UID, JID, CTID, ETID, DID, caseDataContent);

        assertAll(
            () ->  verify(draftGateway).update(caseDataContentCaptor.capture(), draftIdCaptor.capture()),
            () ->  assertThat(draftIdCaptor.getValue(), is("5")),
            () ->  assertThat(caseDataContentCaptor.getValue().document, hasProperty("userId", is(caseDraft.getUserId()))),
            () ->  assertThat(caseDataContentCaptor.getValue().document, hasProperty("jurisdictionId", is(caseDraft.getJurisdictionId()))),
            () ->  assertThat(caseDataContentCaptor.getValue().document, hasProperty("caseTypeId", is(caseDraft.getCaseTypeId()))),
            () ->  assertThat(caseDataContentCaptor.getValue().document, hasProperty("eventTriggerId", is(caseDraft.getEventTriggerId()))),
            () ->  assertThat(caseDataContentCaptor.getValue().document, hasProperty("caseDataContent", is(caseDataContent))),
            () ->  assertThat(caseDataContentCaptor.getValue().type, is(CASE_DATA_CONTENT)),
            () ->  assertThat(result, is(draft))
        );
    }

}
