package uk.gov.hmcts.ccd.v2.external.controller;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.JsonNode;
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
import uk.gov.hmcts.ccd.domain.service.validate.AuthorisedValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.service.validate.OperationContext;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Provider PACT verification for the CCD Data Store "Validate Case Data" endpoint
 * (POST /case-types/{caseTypeId}/validate, served by {@link CaseDataValidatorController}).
 *
 * <p>Verifies any consumer contract published to the broker under the provider name
 * {@code ccdDataStoreAPI_validateCaseData}. {@code @IgnoreNoPactsToVerify} keeps the build
 * green when no such contract exists yet.</p>
 */
@Provider("ccdDataStoreAPI_validateCaseData")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")},
    providerTags = "${pactbroker.providerTags:master}",
    enablePendingPacts = "${pactbroker.enablePending:true}"
)
@TestPropertySource(locations = "/application.properties")
@WebMvcTest({CaseDataValidatorController.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("SECURITY_MOCK")
@ContextConfiguration(classes = {TestIdamConfiguration.class})
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class CaseDataValidatorProviderTest extends WireMockBaseContractTest {

    @MockitoBean
    ApplicationParams applicationParams;

    @MockitoBean
    SecurityUtils securityUtils;

    @MockitoBean
    @Qualifier(AuthorisedValidateCaseFieldsOperation.QUALIFIER)
    ValidateCaseFieldsOperation validateCaseFieldsOperation;

    @Autowired
    CaseDataValidatorController caseDataValidatorController;

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
        testTarget.setControllers(caseDataValidatorController);
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State("A request to validate case data")
    public void setUpValidateCaseData() {
        Map<String, JsonNode> validatedData = Collections.emptyMap();
        when(validateCaseFieldsOperation.validateCaseDetails(any(OperationContext.class)))
            .thenReturn(validatedData);
    }
}
