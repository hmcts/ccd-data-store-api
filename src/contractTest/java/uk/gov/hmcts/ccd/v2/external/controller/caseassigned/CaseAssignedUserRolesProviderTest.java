package uk.gov.hmcts.ccd.v2.external.controller.caseassigned;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.caseaccess.CaseAccessOperation;
import uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController;
import uk.gov.hmcts.ccd.v2.external.controller.TestIdamConfiguration;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Provider("ccdDataStoreAPI_caseAssignedUserRoles")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}", port = "${PACT_BROKER_PORT:80}", consumerVersionSelectors = {
        @VersionSelector(tag = "master")})
@TestPropertySource(locations = "/application.properties")
@WebMvcTest({CaseAssignedUserRolesController.class})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {CaseAssignedUserRolesProviderTestContext.class, TestIdamConfiguration.class})
@IgnoreNoPactsToVerify
@ActiveProfiles("CASE_ASSIGNED")
public class CaseAssignedUserRolesProviderTest extends WireMockBaseTest {

    @Autowired
    ApplicationParams applicationParams;

    @Autowired
    SecurityUtils securityUtils;

    @Autowired
    CaseAssignedUserRolesController caseAssignedUserRolesController;

    @Autowired
    CaseAccessOperation caseAccessOperation;

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
        when(applicationParams.getAuthorisedServicesForCaseUserRoles()).thenReturn(List.of("serviceName"));

        String caseId = "1583841721773828";
        String userId = "0a5874a4-3f38-4bbd-ba4c";
        String caseRole = "[CREATOR]";
        CaseAssignedUserRole caseAssignedUserRole = new CaseAssignedUserRole(caseId, userId, caseRole);
        when(caseAccessOperation.findCaseUserRoles(anyList(), anyList())).thenReturn(List.of(caseAssignedUserRole));
    }
}
