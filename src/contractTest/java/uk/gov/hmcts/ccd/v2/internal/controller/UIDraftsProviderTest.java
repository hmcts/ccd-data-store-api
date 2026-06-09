package uk.gov.hmcts.ccd.v2.internal.controller;

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
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.upsertdraft.UpsertDraftOperation;
import uk.gov.hmcts.ccd.v2.external.controller.TestIdamConfiguration;

/** Provider PACT verification for the internal drafts endpoints (UIDraftsController). */
@Provider("ccdDataStoreAPI_drafts")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")},
    providerTags = "${pactbroker.providerTags:master}",
    enablePendingPacts = "${pactbroker.enablePending:true}")
@TestPropertySource(locations = "/application.properties")
@WebMvcTest({UIDraftsController.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("SECURITY_MOCK")
@ContextConfiguration(classes = {TestIdamConfiguration.class})
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class UIDraftsProviderTest extends WireMockBaseContractTest {

    @MockitoBean
    ApplicationParams applicationParams;
    @MockitoBean
    SecurityUtils securityUtils;
    @MockitoBean
    @Qualifier("default")
    UpsertDraftOperation upsertDraftOperation;
    @MockitoBean
    @Qualifier("defaultDraft")
    GetCaseViewOperation getDraftViewOperation;
    @MockitoBean
    @Qualifier("cached")
    DraftGateway draftGateway;
    @Autowired
    UIDraftsController uiDraftsController;

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
        testTarget.setControllers(uiDraftsController);
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State("A draft is requested for display")
    public void draftRequested() {
        // State setup (mock getDraftViewOperation) to be completed when a consumer contract is published.
    }
}
