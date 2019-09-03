package uk.gov.hmcts.ccd.pact;

import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.spring.SpringRestPactRunner;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ccd.SecurityConfiguration;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;

@Provider("ccd")
//@PactBroker(scheme = "${pact.broker.scheme}",host = "${pact.broker.baseUrl}", port = "${pact.broker.port}", tags={"${pact.broker.consumer.tag}"})
@PactFolder(value = "pact/probate")
@RunWith(SpringRestPactRunner.class)
//@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=8125", "spring.application.name=PACT_TEST"
})
//@EnableAutoConfiguration(exclude = {SecurityConfiguration.class})
@TestPropertySource(locations = "classpath:application-pact.properties")
public class ProbateSubmitServiceProviderTest {

    @MockBean
    private CachedCaseDetailsRepository caseDetailsRepository;

    @MockBean
    private  UIDService uidService;

    @MockBean
    private DefaultUserRepository defaultUserRepository;



	@State("A GrantOfRepresentation case exists")
	public void toCheckGrantOfRepresentationCase654321Exists() {
        when(uidService.validateUID("654321")).thenReturn(true);
        CaseDetails caseDetails = new CaseDetails();
        when(caseDetailsRepository.findByReference(654321L)).thenReturn(caseDetails);
    }


}
