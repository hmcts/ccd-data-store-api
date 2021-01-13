package uk.gov.hmcts.ccd.domain.service.upsertdraft;

import java.util.Map;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDraftBuilder.newCaseDraft;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftResponseBuilder.newDraftResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.EventBuilder.newEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.JurisdictionBuilder.newJurisdiction;
import static uk.gov.hmcts.ccd.domain.service.upsertdraft.DefaultUpsertDraftOperation.CASE_DATA_CONTENT;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

class DefaultUpsertDraftOperationTest {

    public static final int DRAFT_MAX_STALE_DAYS = 7;
    private static final String UID = "1";
    private static final String JID = "TEST";
    private static final String CTID = "TestAddressBookCase";
    private static final String ETID = "createCase";
    private static final String DID = "5";
    private static final CaseTypeDefinition CASE_TYPE = newCaseType()
        .withId(CTID)
        .withJurisdiction(newJurisdiction().withJurisdictionId(JID).build())
        .withEvent(newCaseEvent().withId(ETID).build())
        .withSecurityClassification(SecurityClassification.PUBLIC)
        .build();
    private static final Map<String, JsonNode> DATA = Maps.newHashMap();
    private static final Map<String, JsonNode> SANITISED_DATA = Maps.newHashMap();
    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private DraftGateway draftGateway;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private CaseSanitiser caseSanitiser;
    @Mock
    private UserAuthorisation userAuthorisation;

    private UpsertDraftOperation upsertDraftOperation;

    private CaseDataContent caseDataContent =
        newCaseDataContent().withData(DATA).withEvent(newEvent().withEventId(ETID).build()).build();
    private CaseDraft caseDraft;
    private DraftResponse draftResponse = newDraftResponse().build();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(applicationParams.getDraftMaxTTLDays()).thenReturn(DRAFT_MAX_STALE_DAYS);
        given(caseDefinitionRepository.getCaseType(CTID)).willReturn(CASE_TYPE);
        given(caseSanitiser.sanitise(CASE_TYPE, DATA)).willReturn(SANITISED_DATA);
        given(userAuthorisation.getUserId()).willReturn(UID);

        upsertDraftOperation = new DefaultUpsertDraftOperation(draftGateway, caseDefinitionRepository, caseSanitiser,
            userAuthorisation, applicationParams);
        caseDraft = newCaseDraft()
            .withUserId(UID)
            .withJurisdictionId(JID)
            .withCaseTypeId(CTID)
            .withEventId(ETID)
            .withCaseDataContent(caseDataContent)
            .build();
    }

    @Test
    void shouldSuccessfullySaveDraft() {
        final ArgumentCaptor<CreateCaseDraftRequest> captor = ArgumentCaptor.forClass(CreateCaseDraftRequest.class);
        doReturn(Long.valueOf(DID)).when(draftGateway).create(any(CreateCaseDraftRequest.class));
        draftResponse.setId(DID);

        DraftResponse result = upsertDraftOperation.executeSave(CTID, caseDataContent);

        assertAll(
            () -> verify(draftGateway).create(captor.capture()),
            () -> verify(caseDefinitionRepository).getCaseType(CTID),
            () -> verify(caseSanitiser).sanitise(CASE_TYPE, DATA),
            () -> assertThat(captor.getValue().getDocument(), hasProperty("userId", is(caseDraft.getUserId()))),
            () -> assertThat(captor.getValue().getDocument(), hasProperty("jurisdictionId",
                is(caseDraft.getJurisdictionId()))),
            () -> assertThat(captor.getValue().getDocument(), hasProperty("caseTypeId",
                is(caseDraft.getCaseTypeId()))),
            () -> assertThat(captor.getValue().getDocument(), hasProperty("eventId", is(caseDraft.getEventId()))),
            () -> assertThat(captor.getValue().getDocument(), hasProperty("caseDataContent", is(caseDataContent))),
            () -> assertThat(captor.getValue().getDocument(), hasProperty("caseDataContent",
                hasProperty("data", is(SANITISED_DATA)))),
            () -> assertThat(captor.getValue().getMaxTTLDays(), is(DRAFT_MAX_STALE_DAYS)),
            () -> assertThat(captor.getValue().getType(), is(CASE_DATA_CONTENT)),
            () -> assertThat(result, samePropertyValuesAs(draftResponse))
        );
    }

    @Test
    void shouldSuccessfullyUpdateDraft() {
        final ArgumentCaptor<String> draftIdCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<UpdateCaseDraftRequest> caseDataContentCaptor =
            ArgumentCaptor.forClass(UpdateCaseDraftRequest.class);
        doReturn(draftResponse).when(draftGateway).update(any(UpdateCaseDraftRequest.class), any(String.class));


        DraftResponse result = upsertDraftOperation.executeUpdate(CTID, DID, caseDataContent);

        assertAll(
            () -> verify(draftGateway).update(caseDataContentCaptor.capture(), draftIdCaptor.capture()),
            () -> verify(caseDefinitionRepository).getCaseType(CTID),
            () -> verify(caseSanitiser).sanitise(CASE_TYPE, DATA),
            () -> assertThat(draftIdCaptor.getValue(), is("5")),
            () -> assertThat(caseDataContentCaptor.getValue().getDocument(), hasProperty("userId",
                is(caseDraft.getUserId()))),
            () -> assertThat(caseDataContentCaptor.getValue().getDocument(), hasProperty("jurisdictionId",
                is(caseDraft.getJurisdictionId()))),
            () -> assertThat(caseDataContentCaptor.getValue().getDocument(), hasProperty("caseTypeId",
                is(caseDraft.getCaseTypeId()))),
            () -> assertThat(caseDataContentCaptor.getValue().getDocument(), hasProperty("eventId",
                is(caseDraft.getEventId()))),
            () -> assertThat(caseDataContentCaptor.getValue().getDocument(), hasProperty("caseDataContent",
                is(caseDataContent))),
            () -> assertThat(caseDataContentCaptor.getValue().getDocument(), hasProperty("caseDataContent",
                hasProperty("data", is(SANITISED_DATA)))),
            () -> assertThat(caseDataContentCaptor.getValue().getType(), is(CASE_DATA_CONTENT)),
            () -> assertThat(result, is(draftResponse))
        );
    }

}
