package uk.gov.hmcts.rd;

import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.core.model.annotations.PactFolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.CoreCaseDataApplication;
import uk.gov.hmcts.ccd.WireMockContractBaseTest;

import java.util.Date;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@PactFolder("pacts")
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {CoreCaseDataApplication.class})
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
//        when(securityUtils.authorizationHeadersForDataStoreSystemUser()).thenReturn(createHttpHeaders());
        Thread.sleep(SLEEP_TIME);
    }

    protected HttpHeaders createHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        String s2SToken = generateDummyS2SToken(AUTHORISED_ADD_SERVICE_1);
        headers.add(SERVICE_AUTHORIZATION, "Bearer " + s2SToken);
        return headers;
    }

    public static String generateDummyS2SToken(String serviceName) {
        return Jwts.builder()
            .setSubject(serviceName)
            .setIssuedAt(new Date())
            .signWith(SignatureAlgorithm.HS256, TextCodec.BASE64.encode("AA"))
            .compact();
    }

}
