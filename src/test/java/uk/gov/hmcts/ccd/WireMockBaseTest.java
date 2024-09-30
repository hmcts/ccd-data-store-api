package uk.gov.hmcts.ccd;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@AutoConfigureWireMock(port = 0)
public abstract class WireMockBaseTest extends AbstractBaseIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockBaseTest.class);

    // data values as per: classpath:sql/insert_cases.sql
    public static final String CASE_01_REFERENCE = "1504259907353529";
    public static final String CASE_02_REFERENCE = "1504259907353545";
    public static final String CASE_03_REFERENCE = "1504259907353537";
    public static final String CASE_04_REFERENCE = "1504259907353552";
    public static final String CASE_13_REFERENCE = "1504259907353651";
    public static final String CASE_14_REFERENCE = "1504259907353598";
    public static final String CASE_19_REFERENCE = "1601933818308168";
    public static final String CASE_21_REFERENCE = "9816494993793181";
    public static final String CASE_22_REFERENCE = "3393027116986763";
    public static final Long CASE_01_ID = 1L;
    public static final Long CASE_02_ID = 2L;
    public static final Long CASE_03_ID = 3L;
    public static final Long CASE_04_ID = 4L;
    public static final Long CASE_13_ID = 13L;
    public static final Long CASE_14_ID = 14L;
    public static final Long CASE_19_ID = 19L;
    public static final Long CASE_21_ID = 21L;
    public static final Long CASE_22_ID = 22L;
    public static final String CASE_01_TYPE = "TestAddressBookCase";
    public static final String CASE_02_TYPE = "TestAddressBookCase";
    public static final String CASE_03_TYPE = "TestAddressBookCase";
    public static final int NUMBER_OF_CASES = 23;
    public static final JSONObject responseJson1 = new JSONObject("""
        {
            "user_task": {
                "task_data": {
                    "task_id": "0001",
                    "task_name": "Task 1 - description 1"
                },
                "complete_task": "false"
            }
        }
        """);
    public static final JSONObject responseJson2 = new JSONObject("""
        {
            "user_task": {
                "task_data": {
                    "task_id": "00002",
                    "task_name": "Task 2 as modified by callback",
                },
                "complete_task": "true"
            }
        }
        """);
    private static final String BEARER = "Bearer ";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODI2MDAyMzN9"
        + ".Lz467pTdzRF0MGQye8QDzoLLY_cxk79ZB3OOYdOR-0PGYK5sVay4lxOvhIa-1VnfizaaDDZUwmPdMwQOUBfpBQ";
    private static final String SERVICE_AUTHORISATION_VALUE = BEARER + TOKEN;

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    protected String hostUrl;

    @Inject
    protected ApplicationParams applicationParams;

    @Inject
    protected WireMockServer wireMockServer;

    @Before
    @BeforeEach
    public void initMock() throws IOException {
        super.initMock();
        hostUrl = "http://localhost:" + wiremockPort;

        LOG.info("Wire mock test, host url is {}", hostUrl);

        ReflectionTestUtils.setField(applicationParams, "caseDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "uiDefinitionHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "userProfileHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "draftHost", hostUrl);
        ReflectionTestUtils.setField(applicationParams, "roleAssignmentServiceHost", hostUrl);
    }

    public void stubFor(MappingBuilder mappingBuilder) {
        wireMockServer.stubFor(mappingBuilder);
    }

    public void verifyWireMock(int count, RequestPatternBuilder postRequestedFor) {
        wireMockServer.verify(count, postRequestedFor);
    }

    public void stubSuccess(final String path, final String payload, final UUID mappingId) {
        stubFor(get(urlPathEqualTo(path))
            .withId(mappingId)
            .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
            .willReturn(aResponse()
                .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withStatus(HttpStatus.OK.value())
                .withBody(payload)
            )
        );
    }

    public void stubUpstreamFault(final String path, final UUID mappingId) {
        stubFor(get(urlPathEqualTo(path))
            .withId(mappingId)
            .willReturn(aResponse()
                .withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    }

    public void editSuccessStub(final String path, final String payload, final UUID mappingId) {
        wireMockServer.editStub(get(urlPathEqualTo(path))
            .withId(mappingId)
            .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
            .willReturn(aResponse()
                .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withStatus(HttpStatus.OK.value())
                .withBody(payload)
            )
        );
    }

    public void stubNotFound(final String path, final UUID mappingId) {
        stubFor(get(urlPathEqualTo(path))
            .withId(mappingId)
            .withHeader(SERVICE_AUTHORIZATION, equalTo(SERVICE_AUTHORISATION_VALUE))
            .willReturn(aResponse()
                .withStatus(HttpStatus.NOT_FOUND.value())
            )
        );
    }

    public String objectToJsonString(final Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Configuration
    static class WireMockTestConfiguration {

        @Bean
        public WireMockConfigurationCustomizer wireMockConfigurationCustomizer() {
            return config -> config.extensions(new ResponseDefinitionTransformer() {

                @Override
                public String getName() {
                    return "keep-alive-disabler";
                }

                @Override
                public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition,
                                                    FileSource files, Parameters parameters) {
                    return ResponseDefinitionBuilder.like(responseDefinition)
                        .withHeader(HttpHeaders.CONNECTION, "close")
                        .build();
                }
            });
        }
    }

    protected void stubUserInfo(String userId) {
        stubUserInfo(userId, "caseworker", "caseworker-test", "caseworker-probate-public", "caseworker-probate",
            "caseworker-divorce", "caseworker-sscs");
    }

    protected void stubUserInfo(String userId, String... roles) {
        String rolesList = Stream.of(roles)
            .map(e -> "\"" + e + "\"")
            .collect(Collectors.joining(","));
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .willReturn(okJson("{"
                + "      \"uid\": \"" + userId + "\","
                + "      \"sub\": \"Cloud.Strife@test.com\","
                + "      \"roles\": [ " + rolesList + " ]"
                + "    }").withStatus(200)));
    }

    protected void stubIdamRolesForUser(String userId) {
        stubFor(WireMock.get(urlMatching("/api/v1/users/" + userId))
            .willReturn(okJson("{"
                + "      \"id\": \" " + userId + "\","
                + "      \"email\": \"Cloud.Strife@test.com\","
                + "      \"forename\": \"Cloud\","
                + "      \"surname\": \"Strife\","
                + "      \"roles\": [ \"caseworker\", \"caseworker-test\", \"caseworker-probate-public\","
                + " \"caseworker-probate\", \"caseworker-divorce\", \"caseworker-sscs\" ]"
                + "    }").withStatus(200)));
    }
}
