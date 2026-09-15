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
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.domain.model.std.validator.SupplementaryDataUpdateRequestValidator;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkRetrievalService;
import uk.gov.hmcts.ccd.domain.service.caselinking.GetLinkedCasesResponseCreator;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.SupplementaryDataUpdateOperation;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Provider PACT verification for POST /cases/{caseId}/supplementary-data (CaseController).
 * Verifies consumer contracts from ia-case-api / ia-bail-case-api
 * (state "Supplementary data updated successfully"). NB: the endpoint wraps the updated values in a
 * supplementary_data object; a contract expecting root-level values will surface as genuine feedback.
 */
@Provider("ccdDataStoreAPI_supplementaryUpdate")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")},
    providerTags = "${pactbroker.providerTags:master}",
    enablePendingPacts = "${pactbroker.enablePending:true}")
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class CaseSupplementaryDataProviderTest {

    @Mock
    private GetCaseOperation getCaseOperation;

    @Mock
    private CreateEventOperation createEventOperation;

    @Mock
    private CreateCaseOperation createCaseOperation;

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private GetEventsOperation getEventsOperation;

    @Mock
    private SupplementaryDataUpdateOperation supplementaryDataUpdateOperation;

    @Mock
    private SupplementaryDataUpdateRequestValidator requestValidator;

    @Mock
    private CaseLinkRetrievalService caseLinkRetrievalService;

    @Mock
    private GetLinkedCasesResponseCreator getLinkedCasesResponseCreator;


    @BeforeEach
    void before(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new CaseController(
            getCaseOperation, createEventOperation, createCaseOperation, caseReferenceService,
            getEventsOperation, supplementaryDataUpdateOperation, requestValidator, caseLinkRetrievalService,
            getLinkedCasesResponseCreator));
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


    @State("Supplementary data updated successfully")
    public void supplementaryDataUpdated() {
        when(caseReferenceService.validateUID(anyString())).thenReturn(true);
        when(supplementaryDataUpdateOperation.updateSupplementaryData(anyString(), any()))
            .thenReturn(new SupplementaryData(Map.of("HMCTSServiceId", "some-id")));
    }
}
