package uk.gov.hmcts.ccd.domain.service.createdraft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.draft.DraftRepository;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;

import java.net.URISyntaxException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.service.createdraft.DefaultSaveDraftOperation.CASE_DATA_CONTENT;

class DefaultSaveDraftOperationTest {

    public static final int DRAFT_MAX_STALE_DAYS = 7;
    private static final String UID = "1";
    private static final String JID = "TEST";
    private static final String CTID = "TestAddressBookCase";
    private static final String ETID = "createCase";
    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private DraftRepository draftRepository;

    private SaveDraftOperation saveDraftOperation;

    private CaseDataContent caseDataContent = new CaseDataContent();
    private CaseDataContentDraft caseDataContentDraft;
    private Draft draft = new Draft();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(applicationParams.getDraftMaxStaleDays()).thenReturn(DRAFT_MAX_STALE_DAYS);

        saveDraftOperation = new DefaultSaveDraftOperation(draftRepository, applicationParams);
    }

    @Test
    void shouldSuccessfullySaveDraft() throws URISyntaxException {
        caseDataContentDraft = new CaseDataContentDraft(UID, JID, CTID, ETID, caseDataContent);
        final ArgumentCaptor<CreateCaseDataContentDraft> argument = ArgumentCaptor.forClass(CreateCaseDataContentDraft.class);
        doReturn(draft).when(draftRepository).set(any(CreateCaseDataContentDraft.class));


        Draft result = saveDraftOperation.saveDraft(UID, JID, CTID, ETID, caseDataContent);

        assertAll(
            () ->  verify(draftRepository).set(argument.capture()),
            () ->  assertThat(argument.getValue().document, hasProperty("userId", is(caseDataContentDraft.getUserId()))),
            () ->  assertThat(argument.getValue().document, hasProperty("jurisdictionId", is(caseDataContentDraft.getJurisdictionId()))),
            () ->  assertThat(argument.getValue().document, hasProperty("caseTypeId", is(caseDataContentDraft.getCaseTypeId()))),
            () ->  assertThat(argument.getValue().document, hasProperty("eventTriggerId", is(caseDataContentDraft.getEventTriggerId()))),
            () ->  assertThat(argument.getValue().document, hasProperty("caseDataContent", is(caseDataContent))),
            () ->  assertThat(argument.getValue().maxStaleDays, is(DRAFT_MAX_STALE_DAYS)),
            () ->  assertThat(argument.getValue().type, is(CASE_DATA_CONTENT)),
            () ->  assertThat(result, is(draft))
        );
    }

}
