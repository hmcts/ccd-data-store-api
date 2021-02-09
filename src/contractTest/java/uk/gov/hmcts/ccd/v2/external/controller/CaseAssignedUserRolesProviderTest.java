package uk.gov.hmcts.ccd.v2.external.controller;

import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.target.TestTarget;
import au.com.dius.pact.provider.spring.SpringRestPactRunner;
import au.com.dius.pact.provider.spring.target.MockMvcTarget;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Provider("ccdDataStoreAPI_caseAssignedUserRoles")
@RunWith(SpringRestPactRunner.class)
@PactBroker(scheme = "${pact.broker.scheme}", host = "${pact.broker.baseUrl}",
    port = "${pact.broker.port}", tags = {"${pact.broker.consumer.tag}"})
@TestPropertySource(locations = "/application.properties")
@WebMvcTest({CaseAssignedUserRolesController.class})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {CaseAssignedUserRolesProviderTestContext.class})
public class CaseAssignedUserRolesProviderTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationParams applicationParams;

    @Autowired
    SecurityUtils securityUtils;

    @Autowired
    CaseAssignedUserRolesController caseAssignedUserRolesController;

    @TestTarget
    //Create a new instance of the MockMvcTarget and annotate it as the TestTarget for PactRunner
    public final MockMvcTarget target = new MockMvcTarget();

    @Before
    public void setUp() {
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        target.setControllers(caseAssignedUserRolesController);
    }

    @State("A User Role exists for a Case")
    public void aUserRoleExists() {
        when(securityUtils.getServiceNameFromS2SToken(anyString())).thenReturn("serviceName");
        when(applicationParams.getAuthorisedServicesForCaseUserRoles()).thenReturn(Arrays.asList("serviceName"));
    }
}
