package uk.gov.hmcts.reform.ccd.pactprovider.caseassigneduserroles;

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
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.cauroles.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.reform.ccd.pactprovider.caseassigneduserroles.controller.CasesRestController;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Provider("ccdDataStoreAPI_caseAssignedUserRoles")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")})
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class CasesProviderTest {

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    @Mock
    private SecurityUtils securityUtils;

    @BeforeEach
    void beforeCreate(PactVerificationContext context) {
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new CasesRestController(applicationParams, caseReferenceService,
                                                          caseAssignedUserRolesOperation, securityUtils));
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

    @State("A User Role exists for a Case")
    public void getOrRemoveCaseUserRoles() {
        when(caseReferenceService.validateUID(anyString())).thenReturn(true);
        when(caseAssignedUserRolesOperation.findCaseUserRoles(any(List.class), any(List.class)))
            .thenReturn(mockCaseAssignedUserRoles());
        when(securityUtils.getServiceNameFromS2SToken(anyString())).thenReturn("mockClientServiceName");
        when(applicationParams.getAuthorisedServicesForCaseUserRoles())
            .thenReturn(mockAuthorisedServicesForCaseUserRoles());
    }

    private List<CaseAssignedUserRole> mockCaseAssignedUserRoles() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = new ArrayList<>();
        caseAssignedUserRoles.add(new CaseAssignedUserRole("caseDataId", "userId", "caseRole"));
        return caseAssignedUserRoles;
    }

    private List<String> mockAuthorisedServicesForCaseUserRoles() {
        List<String> authorisedServices = new ArrayList<>();
        authorisedServices.add("mockClientServiceName");
        return authorisedServices;
    }
}
