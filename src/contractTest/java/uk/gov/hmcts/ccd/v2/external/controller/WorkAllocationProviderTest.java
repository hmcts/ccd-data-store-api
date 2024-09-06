package uk.gov.hmcts.ccd.v2.external.controller;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.util.Map;

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Provider("ccdDataStoreAPI_WorkAllocation")
//@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
//    host = "${PACT_BROKER_URL:localhost}",
//    port = "${PACT_BROKER_PORT:9292}", consumerVersionSelectors = {
//        @VersionSelector(tag = "master")})
@PactFolder("target/pacts1")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=8124", "spring.application.name=PACT_TEST",
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
    private static final String CASE_TYPE = "caseType";

    @Autowired
    ContractTestCaseDefinitionRepository contractTestCaseDefinitionRepository;

    @MockBean
    UserAuthorisation userAuthorisation;

    @Autowired
    ContractTestSecurityUtils securityUtils;

    @Autowired
    ContractTestCreateCaseOperation contractTestCreateCaseOperation;

    @Autowired
    ContractTestGetCaseOperation getCaseOperation;

    @Autowired
    ContractTestStartEventOperation startEventOperation;

    @Autowired
    ContractTestCreateEventOperation createEventOperation;

    @Autowired
    ObjectMapper objectMapper;

    private void jcLog(String message) {
        System.out.println("JCDEBUG: WorkAllocationProviderTest: " + message);
        LOG.info("JCDEBUG: WorkAllocationProviderTest: " + message);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        jcLog("pactVerificationTestTemplate() ->");
        if (context != null) {
            context.verifyInteraction();
        }
        jcLog("pactVerificationTestTemplate() <-");
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        jcLog("build() ->");
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", 8123, "/"));
        }
        BaseType.setCaseDefinitionRepository(contractTestCaseDefinitionRepository);
        //System.getProperties().setProperty("pact.verifier.publishResults", "true");
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.ALL);
        when(userAuthorisation.getUserId()).thenReturn("userId");
        jcLog("build() <-");
    }

    @State({"a case exists"})
    public void toGetACase(Map<String, Object> dataMap) {
        jcLog("toGetACase() ->");
        jcLog("toGetACase(): dataMap.size: " + dataMap.size());
        CaseDetails caseDetails = setUpCaseDetailsFromStateMap(dataMap);
        getCaseOperation.setTestCaseReference(caseDetails.getReferenceAsString());
        jcLog("toGetACase() <-");
    }

    private CaseDetails setUpCaseDetailsFromStateMap(Map<String, Object> dataMap) {
        jcLog("setUpCaseDetailsFromStateMap() ->");
        jcLog("setUpCaseDetailsFromStateMap(): dataMap.size: " + dataMap.size());
        Map<String, Object> contentDataMap = (Map<String, Object>) dataMap.get(CASE_DATA_CONTENT);
        jcLog("setUpCaseDetailsFromStateMap(): contentDataMap = "
            + (contentDataMap == null ? "NULL" : contentDataMap.toString()));

        String caseworkerUsername = (String) dataMap.get(CASEWORKER_USERNAME);
        String caseworkerPassword = (String) dataMap.get(CASEWORKER_PASSWORD);
        String caseType = (String) dataMap.get(CASE_TYPE);

        CaseDataContent caseDataContent = objectMapper.convertValue(contentDataMap, CaseDataContent.class);
        jcLog("setUpCaseDetailsFromStateMap(): caseDataContent = "
            + (caseDataContent == null ? "NULL" : caseDataContent.toString()));

        securityUtils.setSecurityContextUserAsCaseworkerByCaseType(caseType, caseworkerUsername,
            caseworkerPassword);
        securityUtils.setSecurityContextUserAsCaseworkerByEvent(caseDataContent.getEventId(), caseworkerUsername,
            caseworkerPassword);
        jcLog("setUpCaseDetailsFromStateMap() <-");
        return contractTestCreateCaseOperation.createCaseDetails(caseType, caseDataContent, true);
    }
}
