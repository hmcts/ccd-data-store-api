package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.clients.PocApiClient;
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
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessGroupUtils;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentTimestampService;
import uk.gov.hmcts.ccd.domain.service.message.MessageContext;
import uk.gov.hmcts.ccd.domain.service.message.MessageService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.ApplicationParams;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
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

    public static final String COMPLEX = "Complex";
    public static final String COLLECTION = "Collection";

    private static final String ON_BEHALF_OF_ID = "24";
    private static final String ON_BEHALF_OF_FNAME = "Pierre OnBehalf";
    private static final String ON_BEHALF_OF_LNAME = "Martin OnBehalf";

    @Mock
    private CaseDetailsRepository caseDetailsRepository;
    @Mock
    private CaseAuditEventRepository caseAuditEventRepository;
    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private CallbackInvoker callbackInvoker;
    @Mock
    private SecurityClassificationServiceImpl securityClassificationService;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private CaseDetails savedCaseDetails;

    @Mock
    private UIDService uidService;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private CaseDocumentService caseDocumentService;

    @Mock
    private CaseDocumentTimestampService caseDocumentTimestampService;

    @Mock
    private PocApiClient pocApiClient;

    @Mock
    private MessageService messageService;

    @Mock
    private CaseDataService caseDataService;

    @InjectMocks
    private SubmitCaseTransaction submitCaseTransaction;
    private Event event;
    private CaseTypeDefinition caseTypeDefinition;
    private IdamUser idamUser;
    private CaseEventDefinition caseEventDefinition;
    private CaseStateDefinition state;
    @Mock
    private ApplicationParams applicationParams;
    private CaseAccessGroupUtils caseAccessGroupUtils;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);

        event = buildEvent();
        caseTypeDefinition = buildCaseType();
        objectMapper = new ObjectMapper();
        caseAccessGroupUtils = new CaseAccessGroupUtils(caseDataService, objectMapper);

        submitCaseTransaction = new SubmitCaseTransaction(caseDetailsRepository,
            caseAuditEventRepository,
            caseTypeService,
            callbackInvoker,
            uidService,
            securityClassificationService,
            caseDataAccessControl,
            messageService,
            caseDocumentService,
            applicationParams,
            caseAccessGroupUtils,
            caseDocumentTimestampService,
            pocApiClient
        );

        idamUser = buildIdamUser();
        caseEventDefinition = buildEventTrigger();
        state = buildState();
        final AboutToSubmitCallbackResponse response = buildResponse();
        doReturn(STATE_ID).when(savedCaseDetails).getState();

        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, STATE_ID);

        doReturn(CASE_UID).when(uidService).generateUID();

        doReturn(caseDetails).when(caseDocumentService).stripDocumentHashes(caseDetails);

        doReturn(savedCaseDetails).when(caseDetailsRepository).set(caseDetails);

        doReturn(CASE_ID).when(savedCaseDetails).getId();

        doReturn(response).when(callbackInvoker).invokeAboutToSubmitCallback(caseEventDefinition,
                                                                             null,
                                                                             this.caseDetails, caseTypeDefinition,
                                                                             IGNORE_WARNING);

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
                                                                               IGNORE_WARNING,
                                                                               null);

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
        final ArgumentCaptor<MessageContext> messageCandidateCaptor = ArgumentCaptor.forClass(MessageContext.class);

        submitCaseTransaction.submitCase(event,
                                         caseTypeDefinition,
                                         idamUser,
                                         caseEventDefinition,
                                         this.caseDetails,
                                         IGNORE_WARNING,
                                         null);

        assertAll(
            () -> verify(caseAuditEventRepository).set(auditEventCaptor.capture()),
            () -> assertAuditEvent(auditEventCaptor.getValue()),
            () -> verify(messageService).handleMessage(messageCandidateCaptor.capture())
        );
    }

    @Test
    @DisplayName("should create a case")
    void shouldPersistCreateCaseEvent() throws IOException {
        CaseDetails inputCaseDetails = new CaseDetails();
        inputCaseDetails.setCaseTypeId("SomeCaseType");
        inputCaseDetails.setJurisdiction("SomeJurisdiction");
        inputCaseDetails.setState("SomeState");
        AboutToSubmitCallbackResponse response = buildResponse();
        doReturn(inputCaseDetails).when(caseDocumentService).stripDocumentHashes(inputCaseDetails);
        doReturn(response).when(callbackInvoker).invokeAboutToSubmitCallback(caseEventDefinition,
            null,
            inputCaseDetails,
            caseTypeDefinition,
            IGNORE_WARNING);

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionDocumentUpload.json");
        inputCaseDetails.setData(dataMap);
        doReturn(inputCaseDetails).when(caseDetailsRepository).set(inputCaseDetails);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, "SomeState");
        doNothing().when(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());

        submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
            idamUser,
            caseEventDefinition,
            inputCaseDetails,
            IGNORE_WARNING,
            null);


        verify(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("should persist event with significant document")
    void shouldPersistEventWithSignificantDocument() {
        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        submitCaseTransaction.submitCase(
            event,
            caseTypeDefinition,
            idamUser,
            caseEventDefinition,
            this.caseDetails,
            IGNORE_WARNING,
            null);

        assertAll(
            () -> verify(caseAuditEventRepository).set(auditEventCaptor.capture()),
            () -> assertAuditEventWithSignificantDocument(auditEventCaptor.getValue())
        );
    }

    @Test
    @DisplayName("should invoke callback")
    void shouldInvokeCallback() {
        submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
            idamUser,
            caseEventDefinition,
            this.caseDetails,
            IGNORE_WARNING,
            null);

        verify(callbackInvoker).invokeAboutToSubmitCallback(caseEventDefinition, null, caseDetails, caseTypeDefinition,
            IGNORE_WARNING);
    }

    @Test
    @DisplayName("should persist event when onBehalfOfUser is Passed")
    void shouldPersistEventWhenOnBehalfOfUserPassed() {
        final ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        final ArgumentCaptor<MessageContext> messageCandidateCaptor = ArgumentCaptor.forClass(MessageContext.class);

        submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
            idamUser,
            caseEventDefinition,
            this.caseDetails,
            IGNORE_WARNING,
            buildOnBehalfOfUser());

        assertAll(
            () -> verify(caseAuditEventRepository).set(auditEventCaptor.capture()),
            () -> assertAuditEventProxyByUser(auditEventCaptor.getValue()),
            () -> verify(messageService).handleMessage(messageCandidateCaptor.capture())
        );
    }

    private void assertAuditEventProxyByUser(final AuditEvent auditEvent) {
        assertAll("Audit event",
            () -> assertThat(auditEvent.getCaseDataId(), is(savedCaseDetails.getId())),
            () -> assertThat(auditEvent.getProxiedBy(), is(IDAM_ID)),
            () -> assertThat(auditEvent.getProxiedByFirstName(), is(IDAM_FNAME)),
            () -> assertThat(auditEvent.getProxiedByLastName(), is(IDAM_LNAME)),
            () -> assertThat(auditEvent.getUserId(), is(ON_BEHALF_OF_ID)),
            () -> assertThat(auditEvent.getUserLastName(), is(ON_BEHALF_OF_LNAME)),
            () -> assertThat(auditEvent.getUserFirstName(), is(ON_BEHALF_OF_FNAME)),
            () -> assertThat(auditEvent.getEventName(), is(EVENT_NAME)),
            () -> assertThat(auditEvent.getCaseTypeId(), is(CASE_TYPE_ID)),
            () -> assertThat(auditEvent.getCaseTypeVersion(), is(VERSION)),
            () -> assertThat(auditEvent.getStateId(), is(STATE_ID)),
            () -> assertThat(auditEvent.getStateName(), is(STATE_NAME)),
            () -> assertThat(auditEvent.getEventId(), is(EVENT_ID)),
            () -> assertThat(auditEvent.getSummary(), is(EVENT_SUMMARY)),
            () -> assertThat(auditEvent.getDescription(), is(EVENT_DESC)));
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

    private void assertCaseData(final CaseDetails caseDetails) {
        assertAll("Assert Casedetails",
            () -> assertThat(caseDetails.getData().get("DocumentField4"), isNotNull()),
            () -> assertThat(caseDetails.getData().get("DocumentField4").get("document_url"), isNotNull()),
            () -> assertThat(caseDetails.getData().get("DocumentField4").get("document_binary_url"), isNotNull())
        );
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

    private IdamUser buildOnBehalfOfUser() {
        final IdamUser idamUser = new IdamUser();
        idamUser.setId(ON_BEHALF_OF_ID);
        idamUser.setForename(ON_BEHALF_OF_FNAME);
        idamUser.setSurname(ON_BEHALF_OF_LNAME);
        return idamUser;
    }

    static HashMap<String, JsonNode> buildCaseData(String fileName) throws IOException {
        InputStream inputStream =
            SubmitCaseTransactionTest.class.getClassLoader().getResourceAsStream("tests/".concat(fileName));

        HashMap<String, JsonNode> result =
            new ObjectMapper().readValue(inputStream, new TypeReference<HashMap<String, JsonNode>>() {
            });

        return result;
    }

}
