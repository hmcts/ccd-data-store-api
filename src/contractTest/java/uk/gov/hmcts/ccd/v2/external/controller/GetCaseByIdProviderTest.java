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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.v2.V2;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Provider PACT verification for GET /cases/{caseId} (CaseController). Verifies the consumer
 * contract published by wa_task_management_api (state "a case exists"). The endpoint requires the
 * experimental header, which the contract does not declare; it is injected into every replayed
 * request via a standalone MockMvc default request header.
 */
@Provider("ccd_data_store_get_case_by_id")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")},
    providerTags = "${pactbroker.providerTags:master}",
    enablePendingPacts = "${pactbroker.enablePending:true}")
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class GetCaseByIdProviderTest {

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
        CaseController caseController = new CaseController(
            getCaseOperation, createEventOperation, createCaseOperation, caseReferenceService,
            getEventsOperation, supplementaryDataUpdateOperation, requestValidator, caseLinkRetrievalService,
            getLinkedCasesResponseCreator);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(caseController)
            .defaultRequest(MockMvcRequestBuilders.get("/").header(V2.EXPERIMENTAL_HEADER, "true"))
            .build();
        MockMvcTestTarget testTarget = new MockMvcTestTarget(mockMvc);
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
