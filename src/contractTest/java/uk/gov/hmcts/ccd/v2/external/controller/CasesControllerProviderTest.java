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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.ccd.auditlog.AuditService;
import uk.gov.hmcts.ccd.data.casedetails.query.UserAuthorisationSecurity;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.search.AuthorisedSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.AuthorisedCaseSearchOperation;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.domain.types.sanitiser.DocumentSanitiser;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@Provider("ccdDataStoreAPI_CaseController")
@RunWith(SpringRestPactRunner.class)
@PactBroker(scheme = "${pact.broker.scheme}", host = "${pact.broker.baseUrl}", port = "${pact.broker.port}", tags = {"${pact.broker.consumer.tag}"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=8123", "spring.application.name=PACT_TEST",
    "ccd.dm.domain=http://dm-store-aat.service.core-compute-aat.internal"
})
@ActiveProfiles("SECURITY_MOCK")
@IgnoreNoPactsToVerify
public class CasesControllerProviderTest {

    private static final String CASEWORKER_USERNAME = "caseworkerUsername";
    private static final String CASEWORKER_PASSWORD = "caseworkerPassword";
    private static final String CASE_DATA_CONTENT = "caseDataContent";
    public static final String JURISDICTION_ID = "jurisdictionId";
    public static final String EVENT_ID = "eventId";

    @Autowired
    ContractTestSecurityUtils securityUtils;

    @MockBean
    UserAuthorisationSecurity userAuthorisationSecurity;

    @Autowired
    ContractTestCreateCaseOperation defaultCreateCaseOperation;

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


    @Autowired
    ContractTestCreateEventOperation createEventOperation;

    @TestTarget
    @SuppressWarnings(value = "VisibilityModifier")
    public final Target target = new HttpTarget("http", "localhost", 8123, "/");


    @Before
    public void setUp() {
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        BaseType.setCaseDefinitionRepository(contractTestCaseDefinitionRepository);
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.ALL);
        when(userAuthorisation.getUserId()).thenReturn("userId");
    }

    @State({"A Read For Citizen is  requested"})
    public void toReadADivorceCaseCitizen(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMap(dataMap);
        getCaseOperation.setTestCaseReference(caseDetails.getReferenceAsString());

    }

    @State({"Read For Caseworker"})
    public void toReadForCaseworker(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMap(dataMap);
        getCaseOperation.setTestCaseReference(caseDetails.getReferenceAsString());

    }

    @State({"SearchCases for Citizen is requested"})
    public void toSearchCasesForACitizen(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMap(dataMap);
        when(elasticsearchCaseSearchOperationMock.execute(any(CrossCaseTypeSearchRequest.class)))
            .thenReturn(new CaseSearchResult(1L, Arrays.asList(caseDetails), null));
    }

    @State({"A Search For Citizen requested"})
    public void toSearchForACitizen(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMap(dataMap);
        when(authorisedSearchOperation.execute(any(MetaData.class), any(Map.class))).thenReturn(Arrays.asList(caseDetails));

    }

    @State({"A StartEvent for Caseworker is  requested"})
    public void toStartEventForACaseworker(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMapForEvent(dataMap);
        startEventOperation.setCaseReferenceOverride((String) dataMap.get(EVENT_ID), caseDetails.getReferenceAsString());

    }

    @State({"A StartEvent for citizen is received"})
    public void toStartEventForACitizen(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMapForEvent(dataMap);
        startEventOperation.setCaseReferenceOverride((String) dataMap.get(EVENT_ID), caseDetails.getReferenceAsString());

    }

    @State({"A Start for Caseworker is requested"})
    public void toStartForACaseworker(Map<String, Object> dataMap) {
        setUpSecurityContextForEvent(dataMap);

    }

    @State({"A Start for Citizen is received"})
    public void toStartForACitizen(Map<String, Object> dataMap) {
        setUpSecurityContextForEvent(dataMap);

    }

    @State({"A SubmitEvent for Caseworker is triggered"})
    public void toSubmitEventForACaseworker(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMapForEvent(dataMap);
        createEventOperation.setTestCaseReference(caseDetails.getReferenceAsString());
    }

    @State({"A SubmitEvent for a Citizen is triggered"})
    public void toSubmitEventForACitizen(Map<String, Object> dataMap) {
        CaseDetails caseDetails = setUpCaseDetailsFromStateMapForEvent(dataMap);
        createEventOperation.setTestCaseReference(caseDetails.getReferenceAsString());
    }

    private CaseDetails setUpCaseDetailsFromStateMap(Map<String, Object> dataMap) {
        Map<String, Object> contentDataMap = (Map<String, Object>) dataMap.get(CASE_DATA_CONTENT);
        String caseworkerUsername = (String) dataMap.get(CASEWORKER_USERNAME);
        String caseworkerPassword = (String) dataMap.get(CASEWORKER_PASSWORD);
        String jurisdictionId = (String) dataMap.get(JURISDICTION_ID);
        securityUtils.setSecurityContextUserAsCaseworkerByJurisdiction(jurisdictionId, caseworkerUsername, caseworkerPassword);
        CaseDataContent caseDataContent = objectMapper.convertValue(contentDataMap, CaseDataContent.class);

        return defaultCreateCaseOperation.createCaseDetails("DIVORCE", caseDataContent, true);

    }

    private CaseDetails setUpCaseDetailsFromStateMapForEvent(Map<String, Object> dataMap) {
        Map<String, Object> contentDataMap = (Map<String, Object>) dataMap.get(CASE_DATA_CONTENT);
        setUpSecurityContextForEvent(dataMap);
        CaseDataContent caseDataContent = objectMapper.convertValue(contentDataMap, CaseDataContent.class);

        return defaultCreateCaseOperation.createCaseDetails("DIVORCE", caseDataContent, true);

    }

    private void setUpSecurityContextForEvent(Map<String, Object> dataMap) {
        String caseworkerUsername = (String) dataMap.get(CASEWORKER_USERNAME);
        String caseworkerPassword = (String) dataMap.get(CASEWORKER_PASSWORD);
        String eventId = (String) dataMap.get(EVENT_ID);
        securityUtils.setSecurityContextUserAsCaseworkerByEvent(eventId, caseworkerUsername, caseworkerPassword);
    }

    private void setUpSecurityContextForJurisdiction(Map<String, Object> dataMap) {
        String caseworkerUsername = (String) dataMap.get(CASEWORKER_USERNAME);
        String caseworkerPassword = (String) dataMap.get(CASEWORKER_PASSWORD);
        String jurisdictionId = (String) dataMap.get(JURISDICTION_ID);
        securityUtils.setSecurityContextUserAsCaseworkerByJurisdiction(jurisdictionId, caseworkerUsername, caseworkerPassword);
    }

}
