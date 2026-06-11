package uk.gov.hmcts.ccd.v2.external.controller;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.WireMockBaseContractTest;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.validator.SupplementaryDataUpdateRequestValidator;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkRetrievalService;
import uk.gov.hmcts.ccd.domain.service.caselinking.GetLinkedCasesResponseCreator;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.SupplementaryDataUpdateOperation;
import uk.gov.hmcts.ccd.v2.V2;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Provider PACT verification for the Retrieve Case by ID endpoint
 * (GET /cases/{caseId}, served by {@link CaseController}).
 *
 * <p>Verifies the consumer contract published by wa_task_management_api under the provider
 * name {@code ccd_data_store_get_case_by_id} (see wa-task-management-api
 * src/contractTest/.../ccd/CcdGetCasesByCaseIdPactTest.java, state "a case exists").</p>
 *
 * <p>The endpoint requires the {@code experimental} header, which the consumer contract does
 * not declare; it is injected into the replayed request via the
 * {@link MockHttpServletRequestBuilder} test-template parameter.</p>
 */
@Provider("ccd_data_store_get_case_by_id")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")},
    providerTags = "${pactbroker.providerTags:master}",
    enablePendingPacts = "${pactbroker.enablePending:true}")
@TestPropertySource(locations = "/application.properties")
@WebMvcTest({CaseController.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("SECURITY_MOCK")
@ContextConfiguration(classes = {TestIdamConfiguration.class})
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class GetCaseByIdProviderTest extends WireMockBaseContractTest {

    @MockitoBean
    ApplicationParams applicationParams;

    @MockitoBean
    SecurityUtils securityUtils;

    @MockitoBean
    @Qualifier("creator")
    GetCaseOperation getCaseOperation;

    @MockitoBean
    @Qualifier("authorised")
    CreateEventOperation createEventOperation;

    @MockitoBean
    @Qualifier("authorised")
    CreateCaseOperation createCaseOperation;

    @MockitoBean
    UIDService caseReferenceService;

    @MockitoBean
    @Qualifier("authorised")
    GetEventsOperation getEventsOperation;

    @MockitoBean
    @Qualifier("authorised")
    SupplementaryDataUpdateOperation supplementaryDataUpdateOperation;

    @MockitoBean
    SupplementaryDataUpdateRequestValidator requestValidator;

    @MockitoBean
    CaseLinkRetrievalService caseLinkRetrievalService;

    @MockitoBean
    GetLinkedCasesResponseCreator getLinkedCasesResponseCreator;

    @Autowired
    CaseController caseController;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context, MockHttpServletRequestBuilder request) {
        if (request != null) {
            request.header(V2.EXPERIMENTAL_HEADER, "true");
        }
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(caseController);
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State("a case exists")
    public void caseExists() {
        when(caseReferenceService.validateUID(anyString())).thenReturn(true);

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(1593694526480034L);
        caseDetails.setJurisdiction("IA");
        caseDetails.setCaseTypeId("Asylum");
        caseDetails.setSecurityClassification(SecurityClassification.PRIVATE);
        caseDetails.setState("appealStarted");

        when(getCaseOperation.execute(anyString())).thenReturn(Optional.of(caseDetails));
    }
}
