package uk.gov.hmcts.reform.ccd.pactprovider.cases;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.reform.ccd.pactprovider.cases.controller.SubmitForCitizenController;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Provider("ccd_submitForCitizen_api")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}")
public class SubmitForCitizenProviderTest {

    @Mock
    private CreateCaseOperation createCaseOperation;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        if (context != null) {
            MockMvcTestTarget testTarget = new MockMvcTestTarget();
            testTarget.setControllers(new SubmitForCitizenController(createCaseOperation));
            context.setTarget(testTarget);
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPactInteractions(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @State("A request to create a case in CCD")
    public void createCaseInCcd() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId("PRLAPPS");
        caseDetails.setState("CaseCreated");
        caseDetails.setSecurityClassification(SecurityClassification.PUBLIC);
        caseDetails.setCreatedDate(LocalDateTime.of(2025, 6, 16, 10, 0));
        caseDetails.setLastModified(LocalDateTime.of(2025, 6, 16, 10, 5));
        caseDetails.setData(Map.of(
            "caseTypeOfApplication",
            new ObjectMapper().valueToTree("C100")
        ));

        when(createCaseOperation.createCaseDetails(
            anyString(),
            nullable(CaseDataContent.class),
            anyBoolean()
        )).thenReturn(caseDetails);
    }
}
