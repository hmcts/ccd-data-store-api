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
import uk.gov.hmcts.ccd.domain.service.common.NewCaseUtils;
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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;


class SubmitCaseTransactionNewCaseTest {

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
        MockitoAnnotations.openMocks(this);

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
            caseDocumentTimestampService
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

        Map<String, JsonNode> dataOrganisation =
            organisationPolicyCaseDataNewCase("OrganisationPolicyField", "caseAssignedField",
            "\"550e8400-e29b-41d4-a716-446655440000\"",true, true);

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
        Event event = anEvent().build();
        event.setEventId(EVENT_ID);
        event.setDescription(EVENT_DESC);
        event.setSummary(EVENT_SUMMARY);
        return event;
    }

    private CaseTypeDefinition buildCaseType() {
        final Version version = new Version();
        version.setNumber(VERSION);
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
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
        IdamUser idamUser = new IdamUser();
        idamUser.setId(IDAM_ID);
        idamUser.setForename(IDAM_FNAME);
        idamUser.setSurname(IDAM_LNAME);
        idamUser.setEmail(IDAM_EMAIL);
        return idamUser;
    }

    static HashMap<String, JsonNode> buildCaseData(String fileName) throws IOException {
        InputStream inputStream =
            SubmitCaseTransactionNewCaseTest.class.getClassLoader()
                .getResourceAsStream("tests/".concat(fileName));

        return new ObjectMapper().readValue(inputStream, new TypeReference<>() {
            });
    }

    @Test
    @DisplayName("should create a case With Organisations for new case false")
    void shouldPersistCreateCaseEventWithOrganisationNewCaseFalse() throws JsonProcessingException {
        Map<String, JsonNode> dataOrganisation =
            organisationPolicyCaseDataNewCase("OrganisationPolicyField","caseAssignedField",
            "\"550e8400-e29b-41d4-a716-446655440000\"", true,false);

        JacksonUtils.merge(JacksonUtils.convertValue(dataOrganisation), inputCaseDetails.getData());

        inputCaseDetails.setSecurityClassification(SecurityClassification.PUBLIC);

        Map<String, JsonNode> caseDataClassificationWithCaseAccessGroup =
            caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(caseDetails, caseTypeDefinition);

        inputCaseDetails.setDataClassification(caseDataClassificationWithCaseAccessGroup);

        doReturn(inputCaseDetails).when(caseDetailsRepository).set(inputCaseDetails);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, "SomeState");
        doNothing().when(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());

        CaseDetails caseDetailsWithSupplementryNewCase = submitCaseTransaction.submitCase(event, caseTypeDefinition,
            idamUser, caseEventDefinition, inputCaseDetails,
            IGNORE_WARNING, null);

        verify(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());
        assertNull(caseDetailsWithSupplementryNewCase.getSupplementaryData());
    }

    @Test
    @DisplayName("should create a case With OrganisationID, multiple organisationProfileField for "
        + "new case false and true")
    void shouldPersistCreateCaseEventWithOrganisationIDCaseMultipleOrganisationProfileFieldNewCase()
        throws JsonProcessingException {

        organisationPolicyMultipleCaseDataNewCase(inputCaseDetails);

        doReturn(inputCaseDetails).when(caseDetailsRepository).set(inputCaseDetails);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, "SomeState");
        doNothing().when(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());

        CaseDetails caseDetailsWithSupplementryNewCase = submitCaseTransaction.submitCase(event, caseTypeDefinition,
            idamUser, caseEventDefinition, inputCaseDetails, IGNORE_WARNING, null);

        verify(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());
        assertCaseDataSupplementry(caseDetailsWithSupplementryNewCase, "\"550e8400-e29b-41d4-a716-446655440000\"");
    }

    @Test
    @DisplayName("should create a case With OrganisationProfileField with new case true")
    void shouldPersistCreateCaseEvenWithOrganisationNewCaseTrue() {

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

        inputCaseDetails.setSecurityClassification(SecurityClassification.PUBLIC);

        Map<String, JsonNode> caseDataClassificationWithCaseAccessGroup =
            caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(caseDetails, caseTypeDefinition);

        inputCaseDetails.setDataClassification(caseDataClassificationWithCaseAccessGroup);

        doReturn(inputCaseDetails).when(caseDetailsRepository).set(inputCaseDetails);
        doReturn(state).when(caseTypeService).findState(caseTypeDefinition, "SomeState");
        doNothing().when(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());

        CaseDetails caseDetailsWithSupplementryNewCase = submitCaseTransaction.submitCase(event,
            caseTypeDefinition,
            idamUser,
            caseEventDefinition,
            inputCaseDetails,
            IGNORE_WARNING,
            null);

        verify(caseDocumentService).attachCaseDocuments(anyString(), anyString(), anyString(), anyList());
        assertCaseDataSupplementry(caseDetailsWithSupplementryNewCase, "\"550e8400-e29b-41d4-a716-446655440000\"");

    }

    private Map<String, JsonNode> organisationPolicyCaseDataNewCase(String orgPolicyField, String role,
                                                                    String organisationId, boolean includeNewCase,
                                                                    boolean newCase)
        throws JsonProcessingException {
        JsonNode data;
        String yesOrNo = "No";
        if (newCase) {
            yesOrNo = "Yes";
        }

        if (includeNewCase) {

            data = MAPPER.readTree(""
                + "{"
                + "  \"Organisation\": {"
                + "    \"OrganisationID\": " + organisationId + ","
                + "    \"OrganisationName\": \"OrganisationName1\""
                + "  },"
                + "  \"OrgPolicyReference\": null,"
                + "  \"OrgPolicyCaseAssignedRole\": \"" + role + "\","
                + "  \"newCase\": \"" + yesOrNo + "\""
                + "}");
        } else {
            data = MAPPER.readTree(""
                + "{"
                + "  \"Organisation\": {"
                + "    \"OrganisationID\": " + organisationId + ","
                + "    \"OrganisationName\": \"OrganisationName1\""
                + "  },"
                + "  \"OrgPolicyReference\": null,"
                + "  \"OrgPolicyCaseAssignedRole\": \"" + role + "\""
                + "}");
        }

        Map<String, JsonNode> result = new HashMap<>();
        result.put(orgPolicyField, data);
        return result;
    }

    private void organisationPolicyMultipleCaseDataNewCase(CaseDetails inputCaseDetails)
        throws JsonProcessingException {

        Map<String, JsonNode> dataOrganisation =
            organisationPolicyCaseDataNewCase("OrganisationPolicyField","caseAssignedField",
            "\"550e8400-e29b-41d4-a716-446655440000\"", true,true);

        JacksonUtils.merge(JacksonUtils.convertValue(dataOrganisation), inputCaseDetails.getData());

        dataOrganisation =
            organisationPolicyCaseDataNewCase("OrganisationPolicyField1","caseAssignedField",
            "\"organisationA\"", false,false);

        JacksonUtils.merge(JacksonUtils.convertValue(dataOrganisation), inputCaseDetails.getData());

        dataOrganisation =
            organisationPolicyCaseDataNewCase("OrganisationPolicyField2","caseAssignedField",
            "\"organisationB\"", true,false);

        JacksonUtils.merge(JacksonUtils.convertValue(dataOrganisation), inputCaseDetails.getData());

        dataOrganisation =
            organisationPolicyCaseDataNewCase("OrganisationPolicyField3","caseAssignedField",
            "\"organisationC\"", true,true);


        JacksonUtils.merge(JacksonUtils.convertValue(dataOrganisation), inputCaseDetails.getData());

    }

    private void assertCaseDataSupplementry(final CaseDetails caseDetails, String organisationId) {
        assertNotNull(caseDetails.getSupplementaryData());

        JsonNode supplementryDataJsonNode = caseDetails.getSupplementaryData()
            .get(NewCaseUtils.SUPPLEMENTRY_DATA_NEW_CASE);
        List<JsonNode> organizationProfiles = NewCaseUtils.findListOfOrganisationPolicyNodesForNewCase(caseDetails,
            NewCaseUtils.CASE_NEW_YES);

        assertAll("Assert CaseDetails, Data, organisationId",
            () -> assertTrue((supplementryDataJsonNode.toString().contains(organisationId))),
            () -> assertTrue(organizationProfiles.isEmpty())
        );
    }

}
