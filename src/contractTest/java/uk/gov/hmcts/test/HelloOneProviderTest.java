package uk.gov.hmcts.test;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Based on GetActorByIdRoleAssignmentProviderTest.java  ,  @Provider("am_roleAssignment_getAssignment")

@ExtendWith(SpringExtension.class)
@Provider("ccdDataStoreAPI_HelloOneProviderTest")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}", port = "${PACT_BROKER_PORT:9292}", consumerVersionSelectors = {
        @VersionSelector(tag = "master")})
@TestPropertySource(locations = "/application.properties")
@IgnoreNoPactsToVerify
public class HelloOneProviderTest {

    private static final Logger LOG = LoggerFactory.getLogger(HelloOneProviderTest.class);

    private void jcLog(String message) {
        LOG.info("JCDEBUG: HelloOneProviderTest: " + message);
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        jcLog("pactVerificationTestTemplate() ->");
        if (context != null) {
            context.verifyInteraction();
        }
        jcLog("pactVerificationTestTemplate() <-");
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        jcLog("before() ->");
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        //System.getProperties().setProperty("pact.verifier.publishResults", "true");
        if (context != null) {
            context.setTarget(testTarget);
        }
        jcLog("before() <-");
    }

    @State({"helloOneTest"})
    public void helloOneTest() throws Exception {
        jcLog("helloOneTest");
    }
}
