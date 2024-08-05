package uk.gov.hmcts.ccd.v2.external.controller;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import java.util.Map;

@ExtendWith(SpringExtension.class)
@Provider("ccdDataStoreAPI_WorkAllocation")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}",
    port = "${PACT_BROKER_PORT:80}", consumerVersionSelectors = {
        @VersionSelector(tag = "master")})
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
public class WorkAllocationProviderTest extends WireMockBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(WorkAllocationProviderTest.class);

    private static final String CASEWORKER_USERNAME = "caseworkerUsername";
    private static final String CASEWORKER_PASSWORD = "caseworkerPassword";
    private static final String CASE_DATA_CONTENT = "caseDataContent";
    public static final String EVENT_ID = "eventId";
    private static final String CASE_TYPE = "caseType";

    @Autowired
    ContractTestSecurityUtils securityUtils;

    @Autowired
    ContractTestCreateCaseOperation contractTestCreateCaseOperation;

    @Autowired
    ContractTestStartEventOperation startEventOperation;

    @Autowired
    ObjectMapper objectMapper;

    private void jcLog(String message) {
        LOG.info("JCDEBUG: WorkAllocationProviderTest: " + message);
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        jcLog("pactVerificationTestTemplate() ->");
        if (context != null) {
            context.verifyInteraction();
        }
        jcLog("pactVerificationTestTemplate() <-");
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        jcLog("before() ->");
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        //System.getProperties().setProperty("pact.verifier.publishResults", "true");
        if (context != null) {
            context.setTarget(testTarget);
        }
        jcLog("before() <-");
    }

    @State({"helloWorldTest"})
    public void helloTwoTest() throws Exception {
        jcLog("helloWorldTest");
    }

    @State({"A Start Event for a Caseworker is  requested"})
    public void toStartEventForACaseworker(Map<String, Object> dataMap) {
        jcLog("A Start Event for a Caseworker is  requested");
        CaseDetails caseDetails = setUpCaseDetailsFromStateMapForEvent(dataMap);
        startEventOperation.setCaseReferenceOverride((String) dataMap.get(EVENT_ID),
            caseDetails.getReferenceAsString());
    }

    @State({"A Start for a Caseworker is requested"})
    public void toStartForACaseworker(Map<String, Object> dataMap) {
        jcLog("A Start for a Caseworker is requested");
        setUpSecurityContextForEvent(dataMap);
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
}
