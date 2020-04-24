package uk.gov.hmcts.ccd.domain.service.createcase;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;

class SubmitCaseTransactionTest {

    private static final String EVENT_ID = "SomeEvent";
    private static final String EVENT_NAME = "Some event";
    private static final String EVENT_SUMMARY = "Some event summary";
    private static final String EVENT_DESC = "Some event description";
    private static final String CASE_TYPE_ID = "TestCaseType";
    private static final Integer VERSION = 67;
    private static final String IDAM_ID = "23";
    private static final String IDAM_FNAME = "Pierre";
    private static final String IDAM_LNAME = "Martin";
    private static final String IDAM_EMAIL = "pmartin@hmcts.test";
    private static final Boolean IGNORE_WARNING = Boolean.TRUE;
    private static final String STATE_ID = "CREATED_ID";
    private static final String STATE_NAME = "Created name";
    private static final String CASE_UID = "1234123412341236";
    private static final String CASE_ID = "45677";
    public static final String DESCRIPTION = "Description";
    public static final String URL = "http://www.yahooo.com";
    public static final SignificantItemType DOCUMENT = SignificantItemType.DOCUMENT;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;
    @Mock
    private CaseAuditEventRepository caseAuditEventRepository;
    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private CallbackInvoker callbackInvoker;
    @Mock
    private SecurityClassificationService securityClassificationService;
    @Mock
    private CaseUserRepository caseUserRepository;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private CaseDetails savedCaseDetails;

    @Mock
    private UIDService uidService;

    @Mock
    private UserAuthorisation userAuthorisation;

    @InjectMocks
    private SubmitCaseTransaction submitCaseTransaction;
    private Event event;
    private CaseTypeDefinition caseTypeDefinition;
    private IdamUser idamUser;
    private CaseEventDefinition caseEventDefinition;
    private CaseStateDefinition state;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);

        submitCaseTransaction = new SubmitCaseTransaction(caseDetailsRepository,
                                                          caseAuditEventRepository,
                                                          caseTypeService,
                                                          callbackInvoker,
                                                          uidService,
                                                          securityClassificationService,
                                                          caseUserRepository,
                                                          userAuthorisation);

        event = buildEvent();
        caseTypeDefinition = buildCaseType();
        idamUser = buildIdamUser();
        caseEventDefinition = buildEventTrigger();
        state = buildState();
        final AboutToSubmitCallbackResponse response = buildResponse();
        doReturn(STATE_ID).when(savedCaseDetails).getState();

        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, STATE_ID);

        doReturn(CASE_UID).when(uidService).generateUID();

        doReturn(savedCaseDetails).when(caseDetailsRepository).set(caseDetails);

        doReturn(CASE_ID).when(savedCaseDetails).getId();

        doReturn(response).when(callbackInvoker).invokeAboutToSubmitCallback(caseEventDefinition,
                                                                             null,
                                                                             this.caseDetails, caseTypeDefinition, IGNORE_WARNING
        );

    }

    private AboutToSubmitCallbackResponse buildResponse() {
        final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();
        aboutToSubmitCallbackResponse.setState(Optional.of("somestring"));
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setType(SignificantItemType.DOCUMENT.name());
        significantItem.setDescription(DESCRIPTION);
        significantItem.setUrl(URL);
        aboutToSubmitCallbackResponse.setSignificantItem(significantItem);
        return aboutToSubmitCallbackResponse;
    }

    private CaseStateDefinition buildState() {
        final CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setName(STATE_NAME);
        return caseStateDefinition;
    }

    @Test
    @DisplayName("should persist case")
    void shouldPersistCase() {
        final CaseDetails actualCaseDetails = submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
                                                                               idamUser,
            caseEventDefinition,
                                                                               this.caseDetails,
                                                                               IGNORE_WARNING);

        final InOrder order = inOrder(caseDetails, caseDetails, caseDetailsRepository);

        assertAll(
            () -> assertThat(actualCaseDetails, sameInstance(savedCaseDetails)),
            () -> order.verify(caseDetails).setCreatedDate(notNull(LocalDateTime.class)),
            () -> order.verify(caseDetails).setLastStateModifiedDate(notNull(LocalDateTime.class)),
            () -> order.verify(caseDetails).setReference(Long.valueOf(CASE_UID)),
            () -> order.verify(caseDetailsRepository).set(caseDetails)
        );
    }

    @Test
    @DisplayName("should persist event")
    void shouldPersistEvent() {
        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
                                         idamUser,
            caseEventDefinition,
                                         this.caseDetails,
                                         IGNORE_WARNING);

        assertAll(
            () -> verify(caseAuditEventRepository).set(auditEventCaptor.capture()),
            () -> assertAuditEvent(auditEventCaptor.getValue())
        );
    }

    @Test
    @DisplayName("should persist event with significant document")
    void shouldPersistEventWithSignificantDocument() {
        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
                                         idamUser,
            caseEventDefinition,
                                         this.caseDetails,
                                         IGNORE_WARNING);

        assertAll(
            () -> verify(caseAuditEventRepository).set(auditEventCaptor.capture()),
            () -> assertAuditEventWithSignificantDocument(auditEventCaptor.getValue())
        );
    }

    @Test
    @DisplayName("when creator has access level GRANTED, then it should grant access to creator")
    void shouldGrantAccessToAccessLevelGrantedCreator() {
        when(userAuthorisation.getAccessLevel()).thenReturn(AccessLevel.GRANTED);

        submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
                                         idamUser,
            caseEventDefinition,
                                         this.caseDetails,
                                         IGNORE_WARNING);

        verify(caseUserRepository).grantAccess(Long.valueOf(CASE_ID), IDAM_ID, CREATOR.getRole());
    }

    @Test
    @DisplayName("when creator has access level ALL, then it should NOT grant access to creator")
    void shouldNotGrantAccessToAccessLevelAllCreator() {
        when(userAuthorisation.getAccessLevel()).thenReturn(AccessLevel.ALL);

        submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
                                         idamUser,
            caseEventDefinition,
                                         this.caseDetails,
                                         IGNORE_WARNING);

        verifyZeroInteractions(caseUserRepository);
    }

    @Test
    @DisplayName("should invoke callback")
    void shouldInvokeCallback() {
        submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
                                         idamUser,
            caseEventDefinition,
                                         this.caseDetails,
                                         IGNORE_WARNING);

        verify(callbackInvoker).invokeAboutToSubmitCallback(caseEventDefinition, null, caseDetails, caseTypeDefinition, IGNORE_WARNING);
    }

    private void assertAuditEvent(final AuditEvent auditEvent) {
        assertAll("Audit event",
            () -> assertThat(auditEvent.getCaseDataId(), is(savedCaseDetails.getId())),
            () -> assertThat(auditEvent.getUserId(), is(IDAM_ID)),
            () -> assertThat(auditEvent.getUserLastName(), is(IDAM_LNAME)),
            () -> assertThat(auditEvent.getUserFirstName(), is(IDAM_FNAME)),
            () -> assertThat(auditEvent.getEventName(), is(EVENT_NAME)),
            () -> assertThat(auditEvent.getCaseTypeId(), is(CASE_TYPE_ID)),
            () -> assertThat(auditEvent.getCaseTypeVersion(), is(VERSION)),
            () -> assertThat(auditEvent.getStateId(), is(STATE_ID)),
            () -> assertThat(auditEvent.getStateName(), is(STATE_NAME)),
            () -> assertThat(auditEvent.getEventId(), is(EVENT_ID)),
            () -> assertThat(auditEvent.getSummary(), is(EVENT_SUMMARY)),
            () -> assertThat(auditEvent.getDescription(), is(EVENT_DESC)));
    }

    private void assertAuditEventWithSignificantDocument(final AuditEvent auditEvent) {
        assertAll("Audit event",
            () -> assertThat(auditEvent.getCaseDataId(), is(savedCaseDetails.getId())),
            () -> assertThat(auditEvent.getUserId(), is(IDAM_ID)),
            () -> assertThat(auditEvent.getUserLastName(), is(IDAM_LNAME)),
            () -> assertThat(auditEvent.getUserFirstName(), is(IDAM_FNAME)),
            () -> assertThat(auditEvent.getEventName(), is(EVENT_NAME)),
            () -> assertThat(auditEvent.getCaseTypeId(), is(CASE_TYPE_ID)),
            () -> assertThat(auditEvent.getCaseTypeVersion(), is(VERSION)),
            () -> assertThat(auditEvent.getStateId(), is(STATE_ID)),
            () -> assertThat(auditEvent.getStateName(), is(STATE_NAME)),
            () -> assertThat(auditEvent.getEventId(), is(EVENT_ID)),
            () -> assertThat(auditEvent.getSummary(), is(EVENT_SUMMARY)),
            () -> assertThat(auditEvent.getDescription(), is(EVENT_DESC)),
            () -> assertThat(auditEvent.getSignificantItem().getType(), is(DOCUMENT.name())),
            () -> assertThat(auditEvent.getSignificantItem().getDescription(), is(DESCRIPTION)),
            () -> assertThat(auditEvent.getSignificantItem().getUrl(), is(URL)));
    }

    private Event buildEvent() {
        final Event event = anEvent().build();
        event.setEventId(EVENT_ID);
        event.setDescription(EVENT_DESC);
        event.setSummary(EVENT_SUMMARY);
        return event;
    }

    private CaseTypeDefinition buildCaseType() {
        final Version version = new Version();
        version.setNumber(VERSION);
        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(CASE_TYPE_ID);
        caseTypeDefinition.setVersion(version);
        return caseTypeDefinition;
    }

    private CaseEventDefinition buildEventTrigger() {
        final CaseEventDefinition event = new CaseEventDefinition();
        event.setId(EVENT_ID);
        event.setName(EVENT_NAME);
        return event;
    }


    private IdamUser buildIdamUser() {
        final IdamUser idamUser = new IdamUser();
        idamUser.setId(IDAM_ID);
        idamUser.setForename(IDAM_FNAME);
        idamUser.setSurname(IDAM_LNAME);
        idamUser.setEmail(IDAM_EMAIL);
        return idamUser;
    }
}
