package uk.gov.hmcts.reform.ccd.pactprovider.cases;

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
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.reform.ccd.pactprovider.cases.controller.GetCaseRestController;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// Get Case Provider Test version 1.

@Provider("ccd_data_store_api_cases")
@PactBroker(
    url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "master")})
@IgnoreNoPactsToVerify
//@PactFolder("target/pacts/reform/employmenttribunals/GetCaseProviderTest")
@ExtendWith(SpringExtension.class)
public class GetCaseProviderTest {

    @Mock
    private GetCaseOperation mockGetCaseOperation;

    @BeforeEach
    void beforeCreate(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new GetCaseRestController(mockGetCaseOperation));
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

    // Mock the interaction.
    @State("A Get Case is requested")
    public void getCase() {
        when(mockGetCaseOperation.execute(anyString())).thenReturn(Optional.of(mockCaseDetails()));
    }

    private CaseDetails mockCaseDetails() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(1L);
        caseDetails.setCaseTypeId("ET_EnglandWales");
        caseDetails.setJurisdiction("EMPLOYMENT");
        caseDetails.setSecurityClassification(SecurityClassification.PUBLIC);
        return caseDetails;
    }
}