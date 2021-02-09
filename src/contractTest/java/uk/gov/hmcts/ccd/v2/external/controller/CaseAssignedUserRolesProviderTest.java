package uk.gov.hmcts.ccd.v2.external.controller;

import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.RestPactRunner;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.target.TestTarget;
import au.com.dius.pact.provider.spring.target.MockMvcTarget;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.service.cauroles.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Provider("ccdDataStoreAPI_caseAssignedUserRoles")
@RunWith(RestPactRunner.class) // Custom pact runner, child of PactRunner which runs only REST tests
@PactBroker(scheme = "${pact.broker.scheme}", host = "${pact.broker.baseUrl}",
    port = "${pact.broker.port}", tags = {"${pact.broker.consumer.tag}"})
public class CaseAssignedUserRolesProviderTest {

    @Mock
    ApplicationParams applicationParams;

    @Mock
    SecurityUtils securityUtils;

    @Mock
    CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    CaseAssignedUserRolesController caseAssignedUserRolesController;

    @TestTarget
    public final MockMvcTarget target = new MockMvcTarget();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        caseAssignedUserRolesController =
            new CaseAssignedUserRolesController(applicationParams, new UIDService(), caseAssignedUserRolesOperation, securityUtils);
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        target.setControllers(caseAssignedUserRolesController);
    }

    @State("A User Role exists for a Case")
    public void aUserRoleExists() {
        when(securityUtils.getServiceNameFromS2SToken(anyString())).thenReturn("serviceName");
        when(applicationParams.getAuthorisedServicesForCaseUserRoles()).thenReturn(Arrays.asList("serviceName"));
    }

}
