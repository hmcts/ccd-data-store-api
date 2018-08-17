package uk.gov.hmcts.ccd.domain.service.createcase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;

import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;
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

    private SubmitCaseTransaction submitCaseTransaction;

    private Event event;
    private CaseType caseType;
    private IDAMProperties idamUser;
    private CaseEvent eventTrigger;
    private CaseState state;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);

        submitCaseTransaction = new SubmitCaseTransaction(caseDetailsRepository,
                                                          caseAuditEventRepository,
                                                          caseTypeService,
                                                          callbackInvoker,
                                                          uidService,
                                                          securityClassificationService,
                                                          caseUserRepository);

        event = buildEvent();
        caseType = buildCaseType();
        idamUser = buildIdamUser();
        eventTrigger = buildEventTrigger();
        state = buildState();

        doReturn(STATE_ID).when(savedCaseDetails).getState();

        doReturn(state).when(caseTypeService).findState(caseType, STATE_ID);

        doReturn(CASE_UID).when(uidService).generateUID();

        doReturn(savedCaseDetails).when(caseDetailsRepository).set(caseDetails);

        doReturn(CASE_ID).when(savedCaseDetails).getId();
    }

    private CaseState buildState() {
        final CaseState caseState = new CaseState();
        caseState.setName(STATE_NAME);
        return caseState;
    }

    @Test
    @DisplayName("should persist case")
    void shouldPersistCase() {
        final CaseDetails actualCaseDetails = submitCaseTransaction.submitCase(event,
                                                                               caseType,
                                                                               idamUser,
                                                                               eventTrigger,
                                                                               this.caseDetails,
                                                                               IGNORE_WARNING);

        final InOrder order = inOrder(caseDetails, caseDetails, caseDetailsRepository);

        assertAll(
            () -> assertThat(actualCaseDetails, sameInstance(savedCaseDetails)),
            () -> order.verify(caseDetails).setCreatedDate(notNull(LocalDateTime.class)),
            () -> order.verify(caseDetails).setReference(Long.valueOf(CASE_UID)),
            () -> order.verify(caseDetailsRepository).set(caseDetails)
        );
    }

    @Test
    @DisplayName("should persist event")
    void shouldPersistEvent() {
        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        submitCaseTransaction.submitCase(event,
                                         caseType,
                                         idamUser,
                                         eventTrigger,
                                         this.caseDetails,
                                         IGNORE_WARNING);

        assertAll(
            () -> verify(caseAuditEventRepository).set(auditEventCaptor.capture()),
            () -> assertAuditEvent(auditEventCaptor.getValue())
        );
    }

    @Test
    @DisplayName("should grant access to creator")
    void shouldGrantAccessToCreator() {
        submitCaseTransaction.submitCase(event,
                                         caseType,
                                         idamUser,
                                         eventTrigger,
                                         this.caseDetails,
                                         IGNORE_WARNING);

        verify(caseUserRepository).grantAccess(Long.valueOf(CASE_ID), IDAM_ID);
    }

    @Test
    @DisplayName("should invoke callback")
    void shouldInvokeCallback() {
        submitCaseTransaction.submitCase(event,
                                         caseType,
                                         idamUser,
                                         eventTrigger,
                                         this.caseDetails,
                                         IGNORE_WARNING);

        verify(callbackInvoker).invokeAboutToSubmitCallback(eventTrigger, null, caseDetails, caseType, IGNORE_WARNING);
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

    private Event buildEvent() {
        final Event event = anEvent().build();
        event.setEventId(EVENT_ID);
        event.setDescription(EVENT_DESC);
        event.setSummary(EVENT_SUMMARY);
        return event;
    }

    private CaseType buildCaseType() {
        final Version version = new Version();
        version.setNumber(VERSION);
        final CaseType caseType = new CaseType();
        caseType.setId(CASE_TYPE_ID);
        caseType.setVersion(version);
        return caseType;
    }

    private CaseEvent buildEventTrigger() {
        final CaseEvent event = new CaseEvent();
        event.setId(EVENT_ID);
        event.setName(EVENT_NAME);
        return event;
    }


    private IDAMProperties buildIdamUser() {
        final IDAMProperties idamUser = new IDAMProperties();
        idamUser.setId(IDAM_ID);
        idamUser.setForename(IDAM_FNAME);
        idamUser.setSurname(IDAM_LNAME);
        idamUser.setEmail(IDAM_EMAIL);
        return idamUser;
    }
}
