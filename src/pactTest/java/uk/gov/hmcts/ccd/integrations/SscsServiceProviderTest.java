package uk.gov.hmcts.ccd.integrations;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
//import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import au.com.dius.pact.provider.spring.SpringRestPactRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.AuthorisedCreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.AuthorisedCreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.search.CreatorSearchOperation;
import uk.gov.hmcts.ccd.domain.service.startevent.AuthorisedStartEventOperation;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.service.ServiceRequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.core.user.UserRequestAuthorizer;

@Provider("ccd")
@PactBroker(scheme = "${pact.broker.scheme}",host = "${pact.broker.baseUrl}", port = "${pact.broker.port}", tags={"${pact.broker.consumer.tag}"})
//@PactFolder(value = "sscs")
@RunWith(SpringRestPactRunner.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=8125", "spring.application.name=PACT_TEST"
})
@TestPropertySource(locations = "classpath:application-pact.properties")
@OverrideAutoConfiguration(enabled = true)
//@EnableAutoConfiguration(exclude = {SecurityConfiguration.class})
public class SscsServiceProviderTest {

    @MockBean
    private CachedCaseDetailsRepository caseDetailsRepository;

    @MockBean
    private UIDService uidService;

    @TestTarget
    @SuppressWarnings(value = "VisibilityModifier")
    public final Target target = new HttpTarget("http", "localhost", 8125, "/");


    private static final String PRINCIPAL = "ccd_data";

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private static final String AUTHORIZATION = "Authorization";

    @MockBean
    private SubjectResolver<Service> serviceResolver;

    @MockBean
    private UserRequestAuthorizer<User> userRequestAuthorizer;

    @MockBean
    ServiceRequestAuthorizer serviceRequestAuthorizer;

    @MockBean
    private HttpServletRequest request;

    @MockBean
    private DefaultUserRepository defaultUserRepository;

    @MockBean
    private CachedUserRepository cachedUserRepository;

    @MockBean
    private CaseUserRepository caseUserRepository;
    @MockBean
    private DefaultCaseDefinitionRepository defaultCaseDefinitionRepository;

    @MockBean
    private CachedCaseDefinitionRepository cachedCaseDefinitionRepository;

    @MockBean
    private CreatorGetCaseOperation getCaseOperation;

    @MockBean
    private AuthorisedStartEventOperation authorisedStartEventOperation;

    @MockBean
    private AuthorisedCreateEventOperation authorisedCreateEventOperation;

    @MockBean
    private AuthorisedCreateCaseOperation authorisedCreateCaseOperation;

    @MockBean
    private ResponseEntity<AfterSubmitCallbackResponse> afterSubmitValue;

    @MockBean
    private CreatorSearchOperation creatorSearchOperation;

    private Service service;

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    //
//    @Value("${pact.broker.version}")
//    private String providerVersion;
//
    @Before
    public void setUpTest() {

        service = new Service(PRINCIPAL);
        when(serviceResolver.getTokenDetails(anyString())).thenReturn(service);
        when(serviceRequestAuthorizer.authorise(any(HttpServletRequest.class))).thenReturn(service);
        Set<String> roles = new HashSet<>();
        User user = new User(PRINCIPAL, roles);
        when(userRequestAuthorizer.authorise(any(HttpServletRequest.class))).thenReturn(user);
        // System.getProperties().setProperty("pact.verifier.publishResults", "true");
        //System.getProperties().setProperty("pact.provider.version", providerVersion);
    }

    @State("CCD reads for a caseworker")
    public void toCheckCaseWorkerRequestFromSSCS() {
        CaseType caseType = TestBuildersUtil.CaseTypeBuilder.newCaseType().withId("1").build();
        when(cachedCaseDefinitionRepository.getCaseType("benefit")).thenReturn(caseType);

        Set<String> roles = new HashSet<>();
        roles.add("ADMIN");
        when(cachedUserRepository.getUserRoles()).thenReturn(roles);

        Map<String, JsonNode> data = new HashMap<>();
        data.put("ccdCaseId", JSON_NODE_FACTORY.textNode("123"));
        data.put("region", JSON_NODE_FACTORY.textNode("asd"));
        CaseDetails caseDetails = TestBuildersUtil.CaseDetailsBuilder.newCaseDetails().withId("654321")
            .withJurisdiction("PROBATE")
            .withCaseTypeId("BENEFIT")
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withData(data)
            .withReference(654321L)
            .build();
        caseDetails.setState("Draft");

        when(afterSubmitValue.getStatusCodeValue()).thenReturn(200);
        AfterSubmitCallbackResponse afterSubmitCallbackresponse = new AfterSubmitCallbackResponse();
        afterSubmitCallbackresponse.setConfirmationBody("somebody");
        when(afterSubmitValue.getBody()).thenReturn(afterSubmitCallbackresponse);
        caseDetails.setAfterSubmitCallbackResponseEntity(afterSubmitValue);
        List<CaseDetails> caseDetailsList = new ArrayList<>();
        caseDetailsList.add(caseDetails);
        when(creatorSearchOperation.execute(any(MetaData.class), any(Map.class))).thenReturn(caseDetailsList);
    }

    @State("CCD searches for a caseworker")
    public void toCheckCaseWorkerSearchRequest() {
        Map<String, JsonNode> data = new HashMap<>();
        data.put("ccdCaseId", JSON_NODE_FACTORY.textNode("123"));
        data.put("region", JSON_NODE_FACTORY.textNode("asd"));
        CaseDetails caseDetails = TestBuildersUtil.CaseDetailsBuilder.newCaseDetails().withId("654321")
            .withJurisdiction("PROBATE")
            .withCaseTypeId("BENEFIT")
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withData(data)
            .withReference(654321L)
            .build();
        caseDetails.setState("Draft");
        when(afterSubmitValue.getStatusCodeValue()).thenReturn(200);
        AfterSubmitCallbackResponse afterSubmitCallbackresponse = new AfterSubmitCallbackResponse();
        afterSubmitCallbackresponse.setConfirmationBody("somebody");
        when(afterSubmitValue.getBody()).thenReturn(afterSubmitCallbackresponse);
        caseDetails.setAfterSubmitCallbackResponseEntity(afterSubmitValue);
        Optional<CaseDetails> caseDetailsOptional = Optional.of(caseDetails);
        when(getCaseOperation.execute("1", "benefit", "282")).thenReturn(caseDetailsOptional);

    }

    @State("CCD starts a case for a caseworker")
    public void toCheckStartCaseForCaseWorker() {
        StartEventTrigger startEventTrigger = new StartEventTrigger();
        startEventTrigger.setEventId("GOP_APPEAL_CREATED");
        startEventTrigger.setToken("123234543456");
        when(authorisedStartEventOperation.triggerStartForCaseType("benefit", "APPEAL_CREATED", null)).thenReturn(startEventTrigger);
    }

    @State("CCD starts an event")
    public void toCheckStartEvent() {
        StartEventTrigger startEventTrigger = new StartEventTrigger();
        startEventTrigger.setEventId("GOP_APPEAL_CREATED");
        startEventTrigger.setToken("123234543456");
        when(authorisedStartEventOperation.triggerStartForCase("282", "APPEAL_CREATED", null)).thenReturn(startEventTrigger);

    }

    @State("CCD submits an event for a caseworker")
    public void toCheckEventForCaseWorkerIsSubmitted() {
        Map<String, JsonNode> data = new HashMap<>();
        data.put("ccdCaseId", JSON_NODE_FACTORY.textNode("123"));
        data.put("region", JSON_NODE_FACTORY.textNode("asd"));
        CaseDetails caseDetails = TestBuildersUtil.CaseDetailsBuilder.newCaseDetails().withId("654321")
            .withJurisdiction("PROBATE")
            .withCaseTypeId("BENEFIT")
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withData(data)
            .withReference(654321L)
            .build();
        caseDetails.setState("Draft");
        when(afterSubmitValue.getStatusCodeValue()).thenReturn(200);
        AfterSubmitCallbackResponse afterSubmitCallbackresponse = new AfterSubmitCallbackResponse();
        afterSubmitCallbackresponse.setConfirmationBody("somebody");
        when(afterSubmitValue.getBody()).thenReturn(afterSubmitCallbackresponse);
        caseDetails.setAfterSubmitCallbackResponseEntity(afterSubmitValue);

        LinkedHashMap<String, JsonNode> dataClassification = new LinkedHashMap<>();
        dataClassification.put("some", JSON_NODE_FACTORY.textNode("thing"));
        LinkedHashMap<String, JsonNode> eventData = new LinkedHashMap<>();
        eventData.put("some", JSON_NODE_FACTORY.textNode("thing"));

        when(authorisedCreateEventOperation.createCaseEvent(eq("282"), any(CaseDataContent.class))).thenReturn(caseDetails);
    }

    @State("CCD submits for caseworker")
    public void toCheckSubmitForCaseWorker() {
        Map<String, JsonNode> data = new HashMap<>();
        data.put("ccdCaseId", JSON_NODE_FACTORY.textNode("123"));
        data.put("region", JSON_NODE_FACTORY.textNode("asd"));
        CaseDetails caseDetails = TestBuildersUtil.CaseDetailsBuilder.newCaseDetails().withId("654321")
            .withJurisdiction("PROBATE")
            .withCaseTypeId("BENEFIT")
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withData(data)
            .withReference(654321L)
            .build();
        caseDetails.setState("Draft");
        when(afterSubmitValue.getStatusCodeValue()).thenReturn(200);
        AfterSubmitCallbackResponse afterSubmitCallbackresponse = new AfterSubmitCallbackResponse();
        afterSubmitCallbackresponse.setConfirmationBody("somebody");
        when(afterSubmitValue.getBody()).thenReturn(afterSubmitCallbackresponse);
        caseDetails.setAfterSubmitCallbackResponseEntity(afterSubmitValue);
        when(authorisedCreateCaseOperation.createCaseDetails(eq("1"), eq("1"), eq("benefit"),
            any(CaseDataContent.class), eq(null))).thenReturn(caseDetails);
    }
}
