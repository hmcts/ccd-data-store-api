package uk.gov.hmcts.rd;

import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.core.model.annotations.PactFolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.CoreCaseDataApplication;
import uk.gov.hmcts.ccd.WireMockContractBaseTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@PactFolder("pacts")
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {CoreCaseDataApplication.class})
@ActiveProfiles("SECURITY_MOCK")
public abstract class AbstractCcdConsumerTest extends WireMockContractBaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    protected static final String AUTHORISED_ADD_SERVICE_1 = "ADD_SERVICE_1";
    protected static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    protected static final String SOME_AUTHORIZATION_TOKEN = "Bearer UserAuthToken";
    protected static final String SOME_SERVICE_AUTHORIZATION_TOKEN = "ServiceToken";

    private static final int SLEEP_TIME = 2000;

    @BeforeEach
    public void prepareTest() throws Exception {
        Thread.sleep(SLEEP_TIME);
    }

    protected String loadFile(String fileName) {
        try {
            String filePath = getClass().getClassLoader().getResource(fileName).getPath();
            return FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
