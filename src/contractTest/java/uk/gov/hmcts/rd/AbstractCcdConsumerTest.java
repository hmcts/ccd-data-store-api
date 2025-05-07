package uk.gov.hmcts.rd;

import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.annotations.PactFolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.ccd.CoreCaseDataApplication;

@ExtendWith(PactConsumerTestExt.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PactTestFor(providerName = "referenceData_location", port = "5555")
@SpringBootTest({
    "reference.data.api.url=http://localhost:5555",
})
@ContextConfiguration(classes = {CoreCaseDataApplication.class})
@TestPropertySource(locations = {"/application.properties"})
public abstract class AbstractCcdConsumerTest {

    @Autowired
    private ObjectMapper objectMapper;

    protected static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    protected static final String SOME_AUTHORIZATION_TOKEN = "Bearer UserAuthToken";
    protected static final String SOME_SERVICE_AUTHORIZATION_TOKEN = "ServiceToken";

    private static final int SLEEP_TIME = 2000;

    @BeforeEach
    public void prepareTest() throws Exception {
        Thread.sleep(SLEEP_TIME);
    }

}