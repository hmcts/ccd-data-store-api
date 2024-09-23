package uk.gov.hmcts.reform.ccd.pactprovider.cases;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.reform.ccd.pactprovider.cases.controller.StartRestController;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// Start Provider Test version 1.

@Provider("ccd_data_store_api_cases")
@PactBroker(
    url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "master")})
@IgnoreNoPactsToVerify
//@PactFolder("target/pacts/reform/employmenttribunals/StartProviderTest")
@ExtendWith(SpringExtension.class)
public class StartProviderTest {

    @Mock
    private StartEventOperation startEventOperation;

    @BeforeEach
    void beforeCreate(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new StartRestController(startEventOperation));
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
    @State("A Start case for a Citizen is requested")
    public void startCaseForCitizen() {
        when(startEventOperation.triggerStartForCaseType(anyString(), anyString(), anyBoolean()))
            .thenReturn(mockStartEventResult());
    }

    private StartEventResult mockStartEventResult() {
        StartEventResult startEventResult = new StartEventResult();
        startEventResult.setCaseDetails(mockCaseDetails());
        startEventResult.setToken("someToken");
        startEventResult.setEventId("startAppeal");
        return startEventResult;
    }

    private CaseDetails mockCaseDetails() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(1L);
        caseDetails.setCaseTypeId("Asylum");
        caseDetails.setJurisdiction("IA");
        caseDetails.setData(new HashMap<String, JsonNode>() {{
                put("appealOutOfCountry", new TextNode("No"));
                put("isOutOfCountryEnabled", new TextNode("No"));
            }
        });
        return caseDetails;
    }
}
