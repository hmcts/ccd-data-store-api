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
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.WireMockBaseContractTest;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.service.casefileview.CategoriesAndDocumentsService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;

/** Provider PACT verification for GET /categoriesAndDocuments/{cid} (CaseFileViewController). */
@Provider("ccdDataStoreAPI_caseFileView")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")},
    providerTags = "${pactbroker.providerTags:master}",
    enablePendingPacts = "${pactbroker.enablePending:true}")
@TestPropertySource(locations = "/application.properties")
@WebMvcTest({CaseFileViewController.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("SECURITY_MOCK")
@ContextConfiguration(classes = {TestIdamConfiguration.class})
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class CaseFileViewProviderTest extends WireMockBaseContractTest {

    @MockitoBean
    ApplicationParams applicationParams;
    @MockitoBean
    SecurityUtils securityUtils;
    @MockitoBean
    @Qualifier("creator")
    GetCaseOperation getCaseOperation;
    @MockitoBean
    UIDService caseReferenceService;
    @MockitoBean
    @Qualifier("authorised")
    CreateEventOperation createEventOperation;
    @MockitoBean
    CategoriesAndDocumentsService categoriesAndDocumentsService;
    @Autowired
    CaseFileViewController caseFileViewController;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(caseFileViewController);
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State("Categories and documents exist for a case")
    public void categoriesAndDocumentsExist() {
        // State setup (mock categoriesAndDocumentsService) to be completed when a consumer contract is published.
    }
}
