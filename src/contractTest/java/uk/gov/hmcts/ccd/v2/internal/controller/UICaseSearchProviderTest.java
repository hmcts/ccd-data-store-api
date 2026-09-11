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
import uk.gov.hmcts.ccd.domain.service.search.CaseSearchResultViewGenerator;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchSortService;

/**
 * Provider PACT verification for POST /internal/searchCases (UICaseSearchController).
 */
@Provider("ccdDataStoreAPI_internalSearch")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")},
    providerTags = "${pactbroker.providerTags:master}",
    enablePendingPacts = "${pactbroker.enablePending:true}")
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class UICaseSearchProviderTest {

    @Mock
    private CaseSearchOperation caseSearchOperation;

    @Mock
    private ElasticsearchQueryHelper elasticsearchQueryHelper;

    @Mock
    private CaseSearchResultViewGenerator caseSearchResultViewGenerator;

    @Mock
    private ElasticsearchSortService elasticsearchSortService;


    @BeforeEach
    void before(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new UICaseSearchController(
            caseSearchOperation, elasticsearchQueryHelper, caseSearchResultViewGenerator, elasticsearchSortService));
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


    @State("A search for cases is requested")
    public void searchForCasesRequested() {
        // State setup (mock caseSearchOperation) to be completed when a consumer contract is published.
    }
}
