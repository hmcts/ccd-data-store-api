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
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.validator.SupplementaryDataUpdateRequestValidator;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkRetrievalService;
import uk.gov.hmcts.ccd.domain.service.caselinking.GetLinkedCasesResponseCreator;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.SupplementaryDataUpdateOperation;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Provider PACT verification for the Update Case Supplementary Data endpoint
 * (POST /cases/{caseId}/supplementary-data, served by {@link CaseController}).
 *
 * <p>Verifies consumer contracts published under the provider name
 * {@code ccdDataStoreAPI_supplementaryUpdate} (consumers: ia_caseApi, ia-bail-case-api;
 * see e.g. ia-case-api src/contractTest/.../ccd/CcdSupplementaryConsumerTest.java).</p>
 *
 * <p>NB: the endpoint responds with the updated values wrapped in a
 * {@code supplementary_data} object. If the consumer contract expects the values at the
 * root of the body, verification will report a body mismatch — that is genuine contract
 * feedback for the consumer team rather than a provider-test defect.</p>
 */
@Provider("ccdDataStoreAPI_supplementaryUpdate")
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
public class CaseSupplementaryDataProviderTest extends WireMockBaseContractTest {

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
    void pactVerificationTestTemplate(PactVerificationContext context) {
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

    @State("Supplementary data updated successfully")
    public void supplementaryDataUpdated() {
        when(caseReferenceService.validateUID(anyString())).thenReturn(true);
        when(supplementaryDataUpdateOperation.updateSupplementaryData(anyString(), any()))
            .thenReturn(new SupplementaryData(Map.of("HMCTSServiceId", "some-id")));
    }
}
