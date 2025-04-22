package uk.gov.hmcts.ccd.v2.external.controller;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditService;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.query.UserAuthorisationSecurity;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkService;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CasePostStateService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.SubmitCaseTransaction;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentAmApiClient;
import uk.gov.hmcts.ccd.domain.service.message.MessageService;
import uk.gov.hmcts.ccd.domain.service.search.AuthorisedSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.AuthorisedCaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.sanitiser.DocumentSanitiser;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.reform.idam.client.models.AuthenticateUserResponse;
import uk.gov.hmcts.reform.idam.client.models.TokenExchangeResponse;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(SpringExtension.class)
@Provider("ccdDataStoreAPI_Cases")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "master")})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=8123", "spring.application.name=PACT_TEST",
    "ccd.document.url.pattern=${CCD_DOCUMENT_URL_PATTERN:https?://(((?:api-gateway.preprod.dm.reform.hmcts.net|"
        + "(dm-store-aat.service.core-compute-aat|dm-store-(pr-[0-9]+|preview).service.core-compute-preview)."
        + "internal(?::d+)?)/documents/[A-Za-z0-9-]+(?:/binary)?)|((em-hrs-api-aat.service.core-compute-aat|"
        + "em-hrs-api-(pr-[0-9]+|preview).service.core-compute-preview).internal(?::d+)?/hearing-recordings/"
        + "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/segments/[0-9]+))}"

})
@TestPropertySource(locations = "/application.properties")
@ActiveProfiles("SECURITY_MOCK")
@IgnoreNoPactsToVerify
public class CasesControllerProviderTest extends WireMockBaseTest {

    private static final String CASEWORKER_USERNAME = "caseworkerUsername";
    private static final String CASEWORKER_PASSWORD = "caseworkerPassword";
    private static final String CASE_DATA_CONTENT = "caseDataContent";
    public static final String JURISDICTION_ID = "jurisdictionId";
    public static final String EVENT_ID = "eventId";
    private static final String CASE_TYPE = "caseType";

    @Autowired
    ContractTestSecurityUtils securityUtils;

    @MockBean
    UserAuthorisationSecurity userAuthorisationSecurity;

    @Autowired
    ContractTestCreateCaseOperation contractTestCreateCaseOperation;

    @Autowired
    ContractTestGetCaseOperation getCaseOperation;

    @Autowired
    ContractTestStartEventOperation startEventOperation;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    EventTokenService eventTokenServiceMock;

    @MockBean
    DocumentSanitiser documentSanitiser;

    @Autowired
    ContractTestCaseDefinitionRepository contractTestCaseDefinitionRepository;

    @MockBean
    AuthorisedCaseSearchOperation elasticsearchCaseSearchOperationMock;

    @MockBean
    AuthorisedSearchOperation authorisedSearchOperation;

    @MockBean
    UserAuthorisation userAuthorisation;

    @MockBean
    AuditService auditService;

    @MockBean
    CaseAccessService caseAccessService;
    @MockBean
    AccessControlService accessControlService;

    @MockBean
    TelemetryClient telemetryClient;

    @Autowired
    ContractTestCreateEventOperation createEventOperation;

    @MockBean
    CaseDataService caseDataService;

    @MockBean
    MessageService messageService;

    @MockBean
    CaseTypeService caseTypeService;

    @MockBean
    CaseAuditEventRepository caseAuditEventRepository;

    @MockBean
    CaseDocumentAmApiClient caseDocumentAmApiClient;

    @MockBean
    ValidateCaseFieldsOperation validateCaseFieldsOperation;

    @MockBean
    SubmitCaseTransaction submitCaseTransaction;

    @MockBean
    TimeToLiveService timeToLiveService;

    @MockBean
    CasePostStateService casePostStateService;

    @MockBean
    CaseLinkService caseLinkService;

    @MockBean
    @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
    CaseDetailsRepository caseDetailsRepository;

    @MockBean
    UIDService uidService;

    @MockBean
    EventTriggerService eventTriggerService;

    @MockBean
    SecurityClassificationServiceImpl securityClassificationService;


    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) throws JsonProcessingException {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", 8123, "/"));
        }
        BaseType.setCaseDefinitionRepository(contractTestCaseDefinitionRepository);
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.ALL);
        when(userAuthorisation.getUserId()).thenReturn("userId");

        AuthenticateUserResponse authenticateUserResponse = new AuthenticateUserResponse("200");

        stubFor(WireMock.post(urlMatching("/oauth2/authorize"))
            .willReturn(okJson(objectMapper.writeValueAsString(authenticateUserResponse)).withStatus(200)));

        TokenExchangeResponse tokenExchangeResponse = new TokenExchangeResponse("some access token");

        stubFor(WireMock.post(urlMatching("/oauth2/token"))
            .willReturn(okJson(objectMapper.writeValueAsString(tokenExchangeResponse)).withStatus(200)));
        when(validateCaseFieldsOperation.validateCaseDetails(any(), any())).thenReturn(new HashMap<>());
    }

    @State("adoption-web makes request to get cases")
    public void adoptionWebToGetCases(Map<String, Object> dataMap) {
    }

    @State("adoption-web makes request to send case event")
    public void adoptionWebToSendCaseEvent(Map<String, Object> dataMap) {
    }

    @State("adoption-web makes request to get citizen-create-application event token")
    public void adoptionWebToGetCitizenCreateEventToken(Map<String, Object> dataMap) {
    }

    @State("adoption-web makes request to get case by id")
    public void adoptionWebToGetCaseById(Map<String, Object> dataMap) {
    }

    @State("adoption-web makes request to create case")
    public void adoptionWebToCreateCase(Map<String, Object> dataMap) {
    }

    @State("adoption-web makes request to get case-users roles")
    public void adoptionWebToGetCaseUsersRoles(Map<String, Object> dataMap) {
    }

    @State("adoption-web makes request to get citizen-update-application event token")
    public void adoptionWebToGetCitizenUpdateEventToken(Map<String, Object> dataMap) {
    }

    @State("A Submit for a Citizen is requested")
    public void probateSubmitForCitizen(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMapForEvent(dataMap);
        createEventOperation.setTestCaseReference(caseDetails.getReferenceAsString());
    }

    @State({"A Get Case is requested"})
    public void toGetACase(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMap(dataMap);
        getCaseOperation.setTestCaseReference(caseDetails.getReferenceAsString());
    }

    @State({"A Read for a Citizen is requested"})
    public void toReadForACitizen(Map<String, Object> dataMap) {
        toGetACase(dataMap);
    }

    @State({"A Read for a Caseworker is requested"})
    public void toReadForCaseworker(Map<String, Object> dataMap) {
        mockCaseDetailsResponse("mock_responses/read_caseworker.json");
        toGetACase(dataMap);
    }

    @State({"A Search for cases is requested"})
    public void toSearchCasesForACitizen(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMap(dataMap);
        when(elasticsearchCaseSearchOperationMock.execute(any(CrossCaseTypeSearchRequest.class), any()))
            .thenReturn(new CaseSearchResult(1L, Arrays.asList(caseDetails), null));
    }

    @State({"A Search cases for a Citizen is requested"})
    public void toSearchForACitizen(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMap(dataMap);
        when(authorisedSearchOperation.execute(any(MetaData.class), any(Map.class)))
            .thenReturn(Arrays.asList(caseDetails));
    }

    @State({"A Start Event for a Caseworker is  requested"})
    public void toStartEventForACaseworker(Map<String, Object> dataMap) {
        mockCaseDetailsResponse("mock_responses/start_event_caseworker.json");
        CaseDetails caseDetails = setUpCaseDetailsFromStateMapForEvent(dataMap);
        startEventOperation.setCaseReferenceOverride((String) dataMap.get(EVENT_ID),
            caseDetails.getReferenceAsString());
    }

    @State({"A Start for a Caseworker is requested"})
    public void toStartForACaseworker(Map<String, Object> dataMap) {
        mockCaseDetailsResponse("mock_responses/read_caseworker.json");
        setUpSecurityContextForEvent(dataMap);
    }

    @State({"A Submit Event for a Caseworker is requested"})
    public void toSubmitEventForACaseworker(Map<String, Object> dataMap) {
        mockCaseDetailsResponse("mock_responses/submit_event_caseworker.json");
        CaseDetails caseDetails = setUpCaseDetailsFromStateMapForEvent(dataMap);
        createEventOperation.setTestCaseReference(caseDetails.getReferenceAsString());
    }

    @State({"A Submit for a Caseworker is requested"})
    public void toSubmitForACaseworker(Map<String, Object> dataMap) {
        mockCaseDetailsResponse("mock_responses/submit_for_caseworker.json");
        setUpSecurityContextForEvent(dataMap);
    }

    private CaseDetails setUpCaseDetailsFromStateMap(Map<String, Object> dataMap) {
        Map<String, Object> contentDataMap = (Map<String, Object>) dataMap.get(CASE_DATA_CONTENT);
        String caseworkerUsername = (String) dataMap.get(CASEWORKER_USERNAME);
        String caseworkerPassword = (String) dataMap.get(CASEWORKER_PASSWORD);
        String caseType = (String) dataMap.get(CASE_TYPE);
        CaseDataContent caseDataContent = objectMapper.convertValue(contentDataMap, CaseDataContent.class);

        securityUtils.setSecurityContextUserAsCaseworkerByCaseType(caseType, caseworkerUsername,
            caseworkerPassword);
        securityUtils.setSecurityContextUserAsCaseworkerByEvent(caseDataContent.getEventId(), caseworkerUsername,
            caseworkerPassword);
        return contractTestCreateCaseOperation.createCaseDetails(caseType, caseDataContent, true);

    }

    private CaseDetails setUpCaseDetailsFromStateMapForEvent(Map<String, Object> dataMap) {
        Map<String, Object> contentDataMap = (Map<String, Object>) dataMap.get(CASE_DATA_CONTENT);
        setUpSecurityContextForEvent(dataMap);
        String caseType = (String) dataMap.get(CASE_TYPE);
        CaseDataContent caseDataContent = objectMapper.convertValue(contentDataMap, CaseDataContent.class);

        return contractTestCreateCaseOperation.createCaseDetails(caseType, caseDataContent, true);

    }

    private void setUpSecurityContextForEvent(Map<String, Object> dataMap) {
        String caseworkerUsername = (String) dataMap.get(CASEWORKER_USERNAME);
        String caseworkerPassword = (String) dataMap.get(CASEWORKER_PASSWORD);
        String eventId = (String) dataMap.get(EVENT_ID);
        String caseTypeId = (String) dataMap.get(CASE_TYPE);
        securityUtils.setSecurityContextUserAsCaseworkerByEvent(eventId, caseworkerUsername, caseworkerPassword);
        securityUtils.setSecurityContextUserAsCaseworkerByCaseType(caseTypeId, caseworkerUsername, caseworkerPassword);
    }

    private CaseDetails mockCaseDetails(String fileName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                log.error("File not found: {}", fileName);
                return null;
            }
            return new ObjectMapper().readValue(inputStream, CaseDetails.class);
        } catch (IOException e) {
            log.error("Error reading file {}: {}", fileName, e.getMessage());
            return null;
        }
    }

    private void mockCaseDetailsResponse(String fileName) {
        CaseDetails caseDetails = mockCaseDetails(fileName);
        when(submitCaseTransaction.submitCase(any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any())).thenReturn(caseDetails);
        when(casePostStateService.evaluateCaseState(any(), any())).thenReturn("");
        when(timeToLiveService.isCaseTypeUsingTTL(any())).thenReturn(false);
        when(timeToLiveService.getUpdatedResolvedTTL(any())).thenReturn(LocalDate.now());
        when(timeToLiveService.updateCaseDetailsWithTTL(any(), any(), any())).thenReturn(caseDetails.getData());
        doNothing().when(caseLinkService).updateCaseLinks(any(), any());
        when(uidService.validateUID(anyString())).thenReturn(true);
        when(caseDetailsRepository.findUniqueCase(any(), any(), any())).thenReturn(caseDetails);
        when(caseDetailsRepository.findByReference(anyString())).thenReturn(Optional.of(caseDetails));
        when(caseDetailsRepository.set(any())).thenReturn(caseDetails);
        when(eventTriggerService.isPreStateValid(any(), any())).thenReturn(true);
        CaseEventDefinition caseEventDefinition = mock(CaseEventDefinition.class);
        when(eventTriggerService.findCaseEvent(any(), any())).thenReturn(caseEventDefinition);
        when(eventTriggerService.isPreStateEmpty(any())).thenReturn(true);
        when(securityClassificationService.getClassificationForEvent(any(), any()))
            .thenReturn(SecurityClassification.PUBLIC);
        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getName()).thenReturn("Created");
        when(caseTypeService.findState(any(), any())).thenReturn(caseStateDefinition);
    }
}
