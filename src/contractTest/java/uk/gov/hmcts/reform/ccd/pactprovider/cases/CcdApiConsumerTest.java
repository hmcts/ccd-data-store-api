package uk.gov.hmcts.reform.ccd.pactprovider.cases;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.reform.ccd.pactprovider.cases.controller.CasesRestController;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

@Slf4j
@ExtendWith(SpringExtension.class)
@Provider("ccd_submitForCitizen_api")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "Dev", consumer = "prl_cos")})
@TestPropertySource(locations = "/application.properties")
@ActiveProfiles("SECURITY_MOCK")
public class CcdApiConsumerTest {

    @Mock
    private GetCaseOperation getCaseOperation;

    @Mock
    private StartEventOperation startEventOperation;

    @Mock
    private CreateEventOperation createEventOperation;

    @Mock
    private CreateCaseOperation mockCreateCaseOperation;

    @BeforeEach
    void beforeCreate(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new CasesRestController(
            getCaseOperation,
            startEventOperation,
            createEventOperation,
            mockCreateCaseOperation
        ));
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

    @State("A request to create a case in CCD")
    public void setupCreateCase() {
        System.setProperty("PACT_TEST_SCENARIO", "CreateCase");

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId("PRLAPPS");
        caseDetails.setState("Submitted");
        caseDetails.setSecurityClassification(SecurityClassification.valueOf("PUBLIC"));
        caseDetails.setCreatedDate(LocalDateTime.of(2025, 6, 16, 10, 0));
        caseDetails.setLastModified(LocalDateTime.of(2025, 6, 16, 10, 5));

        Mockito.when(mockCreateCaseOperation.createCaseDetails(
                anyString(),
                any(CaseDataContent.class),
                anyBoolean()))
            .thenReturn(caseDetails);
    }
}
