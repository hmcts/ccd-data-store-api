package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.clients.PocApiClient;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;
import uk.gov.hmcts.ccd.domain.model.definition.AccessTypeRoleDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroup;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroupWithId;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessGroupUtils;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentTimestampService;
import uk.gov.hmcts.ccd.domain.service.message.MessageService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SubmitCaseTransactionCaseAccessGroupTest {

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

    private static final String CaseAccessGroups = CaseAccessGroupUtils.CASE_ACCESS_GROUPS;

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

    private CaseDetails inputCaseDetails;

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
    private MessageService messageService;

    @Mock
    private CaseDataService caseDataService;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private PocApiClient pocApiClient;

    @InjectMocks
    private SubmitCaseTransaction submitCaseTransaction;
    private Event event;
    private CaseTypeDefinition caseTypeDefinition;
    private IdamUser idamUser;
    private CaseEventDefinition caseEventDefinition;
    private CaseStateDefinition state;

    private ObjectMapper objectMapper;
    private CaseAccessGroupUtils caseAccessGroupUtils;

    @BeforeEach
    void setup() throws IOException {
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

        doReturn(STATE_ID).when(savedCaseDetails).getState();
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, STATE_ID);
        doReturn(CASE_UID).when(uidService).generateUID();
        doReturn(caseDetails).when(caseDocumentService).stripDocumentHashes(caseDetails);
        doReturn(savedCaseDetails).when(caseDetailsRepository).set(caseDetails);
        doReturn(CASE_ID).when(savedCaseDetails).getId();

        objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

        inputCaseDetails = new CaseDetails();
        doReturn(true).when(applicationParams).getCaseGroupAccessFilteringEnabled();
        inputCaseDetails.setCaseTypeId("SomeCaseType");
        inputCaseDetails.setJurisdiction("SomeJurisdiction");
        inputCaseDetails.setState("SomeState");

        AboutToSubmitCallbackResponse response2 = buildResponse();
        doReturn(inputCaseDetails).when(caseDocumentService).stripDocumentHashes(inputCaseDetails);
        doReturn(response2).when(callbackInvoker).invokeAboutToSubmitCallback(caseEventDefinition,
            null, inputCaseDetails, caseTypeDefinition, IGNORE_WARNING);

        AccessTypeRoleDefinition accessTypeRolesDefinition = new AccessTypeRoleDefinition();
        accessTypeRolesDefinition.setCaseTypeId(caseTypeDefinition.getId());
        accessTypeRolesDefinition.setAccessTypeId("someAccessTypeId");
        accessTypeRolesDefinition.setOrganisationalRoleName("someOrgProfileName");
        accessTypeRolesDefinition.setGroupRoleName("GroupRoleName");
        accessTypeRolesDefinition.setCaseAccessGroupIdTemplate("SomeJurisdiction:CIVIL:bulk:"
            + "[RESPONDENT01SOLICITOR]:$ORGID$");
        accessTypeRolesDefinition.setCaseAssignedRoleField("caseAssignedField");

        List<AccessTypeRoleDefinition> accessTypeRolesDefinitions = new ArrayList<AccessTypeRoleDefinition>();
        accessTypeRolesDefinitions.add(accessTypeRolesDefinition);

        accessTypeRolesDefinition.setCaseTypeId(caseTypeDefinition.getId());
        accessTypeRolesDefinition.setAccessTypeId("someAccessTypeId1");
        accessTypeRolesDefinition.setOrganisationalRoleName("someOrgProfileName1");

        AccessTypeRoleDefinition accessTypeRolesDefinition1 = new AccessTypeRoleDefinition();
        accessTypeRolesDefinition1.setGroupRoleName("GroupRoleName1");
        accessTypeRolesDefinition1.setCaseAccessGroupIdTemplate("SomeJurisdictionCIVIL:bulk:"
            + "[RESPONDENT01SOLICITOR]:$ORGID$");
        accessTypeRolesDefinition1.setCaseAssignedRoleField("caseAssignedField");
        accessTypeRolesDefinitions.add(accessTypeRolesDefinition1);

        caseTypeDefinition.setAccessTypeRoleDefinitions(accessTypeRolesDefinitions);

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionDocumentUpload.json");

        inputCaseDetails.setData(dataMap);

        Map<String, JsonNode> dataOrganisation = organisationPolicyCaseData("caseAssignedField",
            "\"550e8400-e29b-41d4-a716-446655440000\"");

        JacksonUtils.merge(JacksonUtils.convertValue(dataOrganisation), inputCaseDetails.getData());

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

    static HashMap<String, JsonNode> buildCaseData(String fileName) throws IOException {
        InputStream inputStream =
            SubmitCaseTransactionCaseAccessGroupTest.class.getClassLoader()
                .getResourceAsStream("tests/".concat(fileName));

        HashMap<String, JsonNode> result =
            new ObjectMapper().readValue(inputStream, new TypeReference<>() {
            });

        return result;
    }

    @Test
    @DisplayName("should create a case With OrganisationID and No CaseAccessGroup")
    void shouldPersistCreateCaseEventWithOrganisationIDNoCaseAccessGroup() {

        inputCaseDetails.setSecurityClassification(SecurityClassification.PUBLIC);

        Map<String, JsonNode> dataCaseAccessGroup = new HashMap<>();
        dataCaseAccessGroup.put(CaseAccessGroups, inputCaseDetails.getData().get(CaseAccessGroups));

        Map<String, JsonNode> caseDataClassificationWithCaseAccessGroup =
            caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(caseDetails, caseTypeDefinition);

        inputCaseDetails.setDataClassification(caseDataClassificationWithCaseAccessGroup);

        doReturn(inputCaseDetails).when(caseDetailsRepository).set(inputCaseDetails);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, "SomeState");
        doNothing().when(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());

        submitCaseTransaction.submitCase(event, caseTypeDefinition, idamUser, caseEventDefinition, inputCaseDetails,
            IGNORE_WARNING, null);

        verify(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("should create a case With OrganisationID, CaseAccessGroup and update CaseAccessGroup")
    void shouldPersistCreateCaseEventWithOrganisationIDCaseAccessGroupUpdateCaseAccessGroup() {

        String caseAccessGroupType = "CCD:all-cases-access";
        String caseAccessGroupID = "SomeJurisdiction:CIVIL:bulk: [RESPONDENT01SOLICITOR]:"
            + " 550e8400-e29b-41d4-a716-446655440000";
        Map<String, JsonNode> dataCaseAccessGroup = caseAccessGroupCaseData(caseAccessGroupType, caseAccessGroupID);

        JacksonUtils.merge(JacksonUtils.convertValue(dataCaseAccessGroup), inputCaseDetails.getData());

        inputCaseDetails.setSecurityClassification(SecurityClassification.PUBLIC);
        dataCaseAccessGroup.put(CaseAccessGroups, inputCaseDetails.getData().get(CaseAccessGroups));

        Map<String, JsonNode> caseDataClassificationWithCaseAccessGroup =
            caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(caseDetails, caseTypeDefinition);

        inputCaseDetails.setDataClassification(caseDataClassificationWithCaseAccessGroup);

        doReturn(inputCaseDetails).when(caseDetailsRepository).set(inputCaseDetails);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, "SomeState");
        doNothing().when(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());

        CaseDetails caseDetailsWithCaseAccessGroup = submitCaseTransaction.submitCase(event, caseTypeDefinition,
            idamUser, caseEventDefinition, inputCaseDetails, IGNORE_WARNING, null);

        verify(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());
        assertCaseDataCaseAccessGroup(caseDetailsWithCaseAccessGroup, 2, caseAccessGroupType);
    }

    @Test
    @DisplayName("should create a case With OrganisationID and any caseAccesstype in CaseAccessGroup")
    void shouldCreateCaseEventWithOrganisationID() {

        String caseAccessGroupType = "Any String";
        String caseAccessGroupID = "SomeJurisdiction:CIVIL:bulk: [RESPONDENT01SOLICITOR]:"
            + " 550e8400-e29b-41d4-a716-446655440000";

        List<CaseAccessGroupWithId> caseAccessGroupForUIs = new ArrayList<>();

        CaseAccessGroup caseAccessGroup = CaseAccessGroup.builder().caseAccessGroupId(caseAccessGroupID)
            .caseAccessGroupType(caseAccessGroupType).build();

        String uuid = UUID.randomUUID().toString();

        CaseAccessGroupWithId caseAccessGroupForUI = CaseAccessGroupWithId.builder()
            .caseAccessGroup(caseAccessGroup)
            .id(uuid).build();
        caseAccessGroupForUIs.add(caseAccessGroupForUI);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode data  = mapper.convertValue(caseAccessGroupForUIs, JsonNode.class);
        Map<String, JsonNode> dataCaseAccessGroup = new HashMap<>();
        dataCaseAccessGroup.put(CaseAccessGroups, data);
        JacksonUtils.merge(JacksonUtils.convertValue(dataCaseAccessGroup), inputCaseDetails.getData());

        inputCaseDetails.setSecurityClassification(SecurityClassification.PUBLIC);

        Map<String, JsonNode> caseDataClassificationWithCaseAccessGroup =
            caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(caseDetails, caseTypeDefinition);

        inputCaseDetails.setDataClassification(caseDataClassificationWithCaseAccessGroup);

        doReturn(inputCaseDetails).when(caseDetailsRepository).set(inputCaseDetails);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, "SomeState");
        doNothing().when(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());

        CaseDetails caseDetailsWithCaseAccessGroup = submitCaseTransaction.submitCase(event, caseTypeDefinition,
            idamUser, caseEventDefinition, inputCaseDetails, IGNORE_WARNING, null);

        verify(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());
        assertCaseDataCaseAccessGroup(caseDetailsWithCaseAccessGroup, 3, caseAccessGroupType);

    }

    @Test
    @DisplayName("should create a case With OrganisationID, CaseAccessGroup caseAccesstype is CCD:all-cases-access")
    void shouldPersistCreateCaseEventWithOrganisationIDUpdateCaseAccessGroup() {

        String caseAccessGroupType = "CCD:all-cases-access";
        String caseAccessGroupID = "SomeJurisdiction:CIVIL:bulk: [RESPONDENT01SOLICITOR]:"
            + " 550e8400-e29b-41d4-a716-446655440000";

        List<CaseAccessGroupWithId> caseAccessGroupForUIs = new ArrayList<>();

        CaseAccessGroup caseAccessGroup = CaseAccessGroup.builder().caseAccessGroupId(caseAccessGroupID)
            .caseAccessGroupType(caseAccessGroupType).build();

        String uuid = UUID.randomUUID().toString();
        CaseAccessGroupWithId caseAccessGroupForUI = CaseAccessGroupWithId.builder()
            .caseAccessGroup(caseAccessGroup)
            .id(uuid).build();
        caseAccessGroupForUIs.add(caseAccessGroupForUI);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode data  = mapper.convertValue(caseAccessGroupForUIs, JsonNode.class);
        Map<String, JsonNode> dataCaseAccessGroup = new HashMap<>();
        dataCaseAccessGroup.put(CaseAccessGroups, data);

        JacksonUtils.merge(JacksonUtils.convertValue(dataCaseAccessGroup), inputCaseDetails.getData());

        inputCaseDetails.setSecurityClassification(SecurityClassification.PUBLIC);

        dataCaseAccessGroup.put(CaseAccessGroups, inputCaseDetails.getData().get(CaseAccessGroups));

        Map<String, JsonNode> caseDataClassificationWithCaseAccessGroup =
            caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(
                caseDetails, caseTypeDefinition);

        inputCaseDetails.setDataClassification(caseDataClassificationWithCaseAccessGroup);

        doReturn(inputCaseDetails).when(caseDetailsRepository).set(inputCaseDetails);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, "SomeState");
        doNothing().when(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());

        CaseDetails caseDetailsWithCaseAccessGroup = submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
            idamUser,
            caseEventDefinition,
            inputCaseDetails,
            IGNORE_WARNING,
            null);

        verify(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());
        assertCaseDataCaseAccessGroup(caseDetailsWithCaseAccessGroup, 2, caseAccessGroupType);

    }

    @Test
    @DisplayName("should create a case With OrganisationID, CaseAccessGroup caseAccesstype"
        + " has CCD:all-cases-access and any string")
    void shouldPersistCreateCaseEventWithOrganisationIDMergeCaseAccessGroup() {

        String caseAccessGroupType = "Any String";
        String caseAccessGroupID = "SomeJurisdiction:CIVIL:bulk: [RESPONDENT01SOLICITOR]:"
            + " 550e8400-e29b-41d4-a716-446655440000";

        List<CaseAccessGroupWithId> caseAccessGroupForUIs = new ArrayList<>();

        CaseAccessGroup caseAccessGroup = CaseAccessGroup.builder().caseAccessGroupId(caseAccessGroupID)
            .caseAccessGroupType(caseAccessGroupType).build();
        String uuid = UUID.randomUUID().toString();

        CaseAccessGroupWithId caseAccessGroupForUI = CaseAccessGroupWithId.builder()
            .caseAccessGroup(caseAccessGroup)
            .id(uuid).build();
        caseAccessGroupForUIs.add(caseAccessGroupForUI);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode data  = mapper.convertValue(caseAccessGroupForUIs, JsonNode.class);
        Map<String, JsonNode> dataCaseAccessGroup = new HashMap<>();
        dataCaseAccessGroup.put(CaseAccessGroups, data);

        JacksonUtils.merge(JacksonUtils.convertValue(dataCaseAccessGroup), inputCaseDetails.getData());
        inputCaseDetails.setSecurityClassification(SecurityClassification.PUBLIC);

        Map<String, JsonNode> caseDataClassificationWithCaseAccessGroup =
            caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(caseDetails, caseTypeDefinition);

        inputCaseDetails.setDataClassification(caseDataClassificationWithCaseAccessGroup);

        doReturn(inputCaseDetails).when(caseDetailsRepository).set(inputCaseDetails);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, "SomeState");
        doNothing().when(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());

        CaseDetails caseDetailsWithCaseAccessGroup = submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
            idamUser,
            caseEventDefinition,
            inputCaseDetails,
            IGNORE_WARNING,
            null);

        verify(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());
        assertCaseDataCaseAccessGroup(caseDetailsWithCaseAccessGroup, 3, caseAccessGroupType);
    }

    private Map<String, JsonNode> organisationPolicyCaseData(String role, String organisationId)
        throws JsonProcessingException {

        JsonNode data = MAPPER.readTree(""
            + "{"
            + "  \"Organisation\": {"
            + "    \"OrganisationID\": " + organisationId + ","
            + "    \"OrganisationName\": \"OrganisationName1\""
            + "  },"
            + "  \"OrgPolicyReference\": null,"
            + "  \"OrgPolicyCaseAssignedRole\": \"" + role + "\""
            + "}");

        Map<String, JsonNode> result = new HashMap<>();
        result.put("OrganisationPolicyField", data);
        return result;
    }

    private Map<String, JsonNode> caseAccessGroupCaseData(String caseAccessGroupType, String caseAccessGroupID) {

        CaseAccessGroup caseAccessGroup = CaseAccessGroup.builder().caseAccessGroupId(caseAccessGroupID)
            .caseAccessGroupType(caseAccessGroupType).build();

        CaseAccessGroupWithId caseAccessGroupForUI = CaseAccessGroupWithId.builder()
            .caseAccessGroup(caseAccessGroup)
            .id(UUID.randomUUID().toString()).build();

        List<CaseAccessGroupWithId> caseAccessGroupForUIs = new ArrayList<>();
        caseAccessGroupForUIs.add(caseAccessGroupForUI);

        CaseAccessGroup caseAccessGroup1 = CaseAccessGroup.builder()
            .caseAccessGroupId("SomeJurisdictionCIVIL:bulk: [RESPONDENT02SOLICITOR]:$ORG$")
            .caseAccessGroupType("CCD:all-cases-access").build();

        CaseAccessGroupWithId caseAccessGroupForUI1 = CaseAccessGroupWithId.builder()
            .caseAccessGroup(caseAccessGroup1)
            .id(UUID.randomUUID().toString()).build();

        caseAccessGroupForUIs.add(caseAccessGroupForUI1);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode data  = mapper.convertValue(caseAccessGroupForUIs, JsonNode.class);
        Map<String, JsonNode> result = new HashMap<>();
        result.put(CaseAccessGroups, data);
        return result;
    }

    private void assertCaseDataCaseAccessGroup(final CaseDetails caseDetails, int size, String caseAccessGroupType) {
        JsonNode caseAccessGroupsJsonNode = caseDetails.getData().get(CaseAccessGroups);
        assertAll("Assert Casedetails, Data, CaseAccessGroups",
            () -> assertEquals(size, caseAccessGroupsJsonNode.size()),
            () -> assertTrue((caseAccessGroupsJsonNode.toString().contains(caseAccessGroupType)))
        );
    }

}
