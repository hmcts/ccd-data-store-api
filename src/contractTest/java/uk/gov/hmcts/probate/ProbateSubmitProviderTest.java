package uk.gov.hmcts.probate;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditService;
import uk.gov.hmcts.ccd.data.casedetails.query.UserAuthorisationSecurity;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.message.MessageService;
import uk.gov.hmcts.ccd.domain.service.search.AuthorisedSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.AuthorisedCaseSearchOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.DocumentSanitiser;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.v2.external.controller.ContractTestCaseDefinitionRepository;
import uk.gov.hmcts.ccd.v2.external.controller.ContractTestCreateCaseOperation;
import uk.gov.hmcts.ccd.v2.external.controller.ContractTestCreateEventOperation;
import uk.gov.hmcts.ccd.v2.external.controller.ContractTestGetCaseOperation;
import uk.gov.hmcts.ccd.v2.external.controller.ContractTestSecurityUtils;
import uk.gov.hmcts.ccd.v2.external.controller.ContractTestStartEventOperation;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Provider("ccdDataStoreAPI_Cases")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}",
    port = "${PACT_BROKER_PORT:80}",
    consumerVersionSelectors = {@VersionSelector(tag = "Dev")})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=8123"
})
@ActiveProfiles("SECURITY_MOCK")
@IgnoreNoPactsToVerify
public class ProbateSubmitProviderTest extends WireMockBaseTest {

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

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", 8123, "/"));
        }
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

    @State({"A Get Case is requested"})
    public void toGetACase(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMap(dataMap);
        getCaseOperation.setTestCaseReference(caseDetails.getReferenceAsString());

    }

    @State("A Get for a Case is requested")
public void getForCaseGivenGetCaseRequested(Map<String, Object> dataMap) {
    toGetACase(dataMap);

}

@State("A Get for a Case is requested given a Get Case is requested")
public void getForCaseGivenGetCaseRequested() {
}

@State("A Start Event for a Caseworker given a Start Event for a Caseworker is requested")
public void startEventForCaseworkerGivenStartEventRequested() {
}

@State("A Start for a Caseworker given a Start for a Caseworker is requested")
public void startForCaseworkerGivenStartRequested() {
}

@State("A Start for a Citizen given a Start for a Citizen is requested")
public void startForCitizenGivenStartRequested() {
}

@State("A Submit Event for a Caseworker given a Submit Event for a Caseworker is requested")
public void submitEventForCaseworkerGivenSubmitEventRequested() {
}

@State("A Submit Event for a Citizen given a Submit Event for a Citizen is requested")
public void submitEventForCitizenGivenSubmitEventRequested() {
}

@State("A Submit for a Caseworker given a Submit for a Caseworker is requested")
public void submitForCaseworkerGivenSubmitRequested() {
}

@State("A Submit for a Citizen given a Submit for a Citizen is requested")
public void submitForCitizenGivenSubmitRequested() {
}

}