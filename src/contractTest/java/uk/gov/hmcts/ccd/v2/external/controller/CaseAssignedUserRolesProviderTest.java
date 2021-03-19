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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Provider("ccdDataStoreAPI_caseAssignedUserRoles")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}", port = "${PACT_BROKER_PORT:80}", consumerVersionSelectors = {
    @VersionSelector(tag = "master")})
@TestPropertySource(locations = "/application.properties")
@WebMvcTest({CaseAssignedUserRolesController.class})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {CaseAssignedUserRolesProviderTestContext.class})
@IgnoreNoPactsToVerify
public class CaseAssignedUserRolesProviderTest {

    @Autowired
    ApplicationParams applicationParams;

    @Autowired
    SecurityUtils securityUtils;

    @Autowired
    CaseAssignedUserRolesController caseAssignedUserRolesController;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        //System.getProperties().setProperty("pact.verifier.publishResults", "true");
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(
            caseAssignedUserRolesController);
        if (context != null) {
            context.setTarget(testTarget);
        }
    }


    @State("A User Role exists for a Case")
    public void setUpUserRoleExists() {
        when(securityUtils.getServiceNameFromS2SToken(anyString())).thenReturn("serviceName");
        when(applicationParams.getAuthorisedServicesForCaseUserRoles()).thenReturn(Arrays.asList("serviceName"));
    }
}
