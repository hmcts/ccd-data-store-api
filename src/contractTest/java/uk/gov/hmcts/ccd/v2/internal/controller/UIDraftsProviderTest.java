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
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.upsertdraft.UpsertDraftOperation;

/**
 * Provider PACT verification for the internal drafts endpoints (UIDraftsController).
 */
@Provider("ccdDataStoreAPI_drafts")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")},
    providerTags = "${pactbroker.providerTags:master}",
    enablePendingPacts = "${pactbroker.enablePending:true}")
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class UIDraftsProviderTest {

    @Mock
    private UpsertDraftOperation upsertDraftOperation;

    @Mock
    private GetCaseViewOperation getDraftViewOperation;

    @Mock
    private DraftGateway draftGateway;


    @BeforeEach
    void before(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new UIDraftsController(upsertDraftOperation, getDraftViewOperation, draftGateway));
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }


    @State("A draft is requested for display")
    public void draftRequested() {
        // State setup (mock getDraftViewOperation) to be completed when a consumer contract is published.
    }
}
