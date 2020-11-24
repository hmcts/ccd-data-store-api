package uk.gov.hmcts.ccd.v2.external.controller;


import au.com.dius.pact.provider.junit.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import au.com.dius.pact.provider.spring.SpringRestPactRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.ccd.auditlog.AuditService;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.query.UserAuthorisationSecurity;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.sanitiser.DocumentSanitiser;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.util.Map;

import static org.mockito.Mockito.when;


@Provider("ccdDataStoreAPI_CaseController")
@RunWith(SpringRestPactRunner.class)
@PactBroker(scheme = "${pact.broker.scheme}", host = "${pact.broker.baseUrl}", port = "${pact.broker.port}", tags = {"${pact.broker.consumer.tag}"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=8123", "spring.application.name=PACT_TEST",
    "ccd.dm.domain=http://dm-store-aat.service.core-compute-aat.internal"
})
@ActiveProfiles("SECURITY_MOCK")
@Import(CasesControllerProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
public class CasesControllerProviderTest {

    @Autowired
    SecurityUtils securityUtils;


    @MockBean
    UserAuthorisationSecurity userAuthorisationSecurity;

    @Autowired
    ContractTestCreateCaseOperation defaultCreateCaseOperation;

    @Autowired
    ContractTestGetCaseOperation getCaseOperation;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    EventTokenService eventTokenServiceMock;

    @MockBean
    DocumentSanitiser documentSanitiser;

    @Autowired
    ContractTestCaseDefinitionRepository contractTestCaseDefinitionRepository;
//
//    @MockBean
//    private ServiceAuthTokenValidator serviceAuthTokenValidatorMock;
//
//    @MockBean
//    ServiceAuthorisationApi serviceAuthorisationApiMock;
;

    @MockBean
    UserAuthorisation userAuthorisation;

    @MockBean
    AuditService auditService;

    @TestTarget
    @SuppressWarnings(value = "VisibilityModifier")
    public final Target target = new HttpTarget("http", "localhost", 8123, "/");



    @Before
    public void setUp() {
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        BaseType.setCaseDefinitionRepository(contractTestCaseDefinitionRepository);
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.ALL);
        // when(serviceAuthorisationApiMock.getServiceName(anyString())).thenReturn("ccd_gw");


    }


    @State({"A Read For Citizen is  requested"})
    public void toReadADivorceCaseCitizen(Map<String, Object> dataMap) {

        securityUtils.setSecurityContextUserAsCaseworker();
        CaseDataContent caseDataContent = objectMapper.convertValue(dataMap, CaseDataContent.class);

        CaseDetails caseDetails = defaultCreateCaseOperation.createCaseDetails("DIVORCE", caseDataContent, true);
        getCaseOperation.setTestCaseReference(caseDetails.getReferenceAsString());


    }


    @State({"Read For Caseworker"})
    public void toReadForCaseworker(Map<String, Object> dataMap) {
//        CaseDataContent caseDataContent = objectMapper.convertValue(dataMap, CaseDataContent.class);
//
//        defaultCreateCaseOperation.createCaseDetails("DIVORCE",  caseDataContent, true);

    }
}
