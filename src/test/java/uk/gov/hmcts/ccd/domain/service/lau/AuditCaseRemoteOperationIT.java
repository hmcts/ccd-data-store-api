package uk.gov.hmcts.ccd.domain.service.lau;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.ccd.AuditCaseRemoteConfiguration;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.auditlog.AuditService;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.lau.ActionLog;
import uk.gov.hmcts.ccd.domain.model.lau.CaseActionPostRequest;
import uk.gov.hmcts.ccd.domain.model.lau.CaseSearchPostRequest;
import uk.gov.hmcts.ccd.domain.model.lau.SearchLog;

import jakarta.inject.Inject;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;

@Import(AuditCaseRemoteOperationIT.MockConfig.class)
public class AuditCaseRemoteOperationIT extends WireMockBaseTest {

    private static int ASYNC_DELAY_TIMEOUT_MILLISECONDS = 2000;
    private static int ASYNC_DELAY_INTERVAL_MILLISECONDS = 1000;

    private static final String EXPECTED_CASE_ACTION_LOG_JSON =
        "{\"actionLog\":{\"userId\":\"1234\",\"caseAction\":\"VIEW\",\"caseRef\":\"1504259907353529\","
        + "\"caseJurisdictionId\":\"PROBATE\",\"caseTypeId\":\"Caveat\",\"timestamp\":\"2018-08-19T16:02:42.010Z\"}}";

    private static final String EXPECTED_CASE_SEARCH_LOG_JSON =
        "{\"searchLog\":{\"userId\":\"1234\",\"caseRefs\":[\"1504259907353529\"],"
        + "\"timestamp\":\"2018-08-19T16:02:42.010Z\"}}";

    private static final String JURISDICTION = "PROBATE";
    private static final String CASE_TYPE = "Caveat";
    private static final String CASE_ID = "1504259907353529";
    private static final String IDAM_ID = "1234";


    @Autowired
    SecurityUtils securityUtils;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private AuditCaseRemoteConfiguration auditCaseRemoteConfiguration;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditRepository auditRepository;

    @MockitoSpyBean
    private AuditCaseRemoteOperation auditCaseRemoteOperation;

    @Inject
    private AuditService auditService;

    @Autowired
    @Qualifier("SimpleObjectMapper")
    ObjectMapper objectMapper;

    private static final String TIMESTAMP_AS_TEXT = "2018-08-19T16:02:42.010Z";
    private static final String SERVICE_AUTHORIZATION_HEADER = "ServiceAuthorization";

    private static final String SEARCH_AUDIT_ENDPOINT = "/audit/caseSearch";
    private static final String ACTION_AUDIT_ENDPOINT = "/audit/caseAction";

    private static final int SEARCH_AUDIT_HTTP_STATUS = 201;
    private static final int ACTION_AUDIT_HTTP_STATUS = 201;
    private static final int AUDIT_UNAUTHORISED_HTTP_STATUS = 401;
    private static final int AUDIT_FORBIDDEN_HTTP_STATUS = 403;

    private static final int AUDIT_BAD_GATEWAY_HTTP_STATUS = 502;
    private static final int AUDIT_GATEWAY_TIMEOUT_HTTP_STATUS = 504;

    private static final String SEARCH_LOG_USER_ID = IDAM_ID;
    private static final String SEARCH_LOG_CASE_REFS = CASE_ID;

    private static final String ACTION_LOG_USER_ID = IDAM_ID;
    private static final String ACTION_LOG_CASE_REF = CASE_ID;
    private static final String ACTION_LOG_CASE_ACTION = "VIEW";
    private static final String ACTION_LOG_CASE_JURISDICTION_ID = JURISDICTION;
    private static final String ACTION_LOG_CASE_TYPE_ID = CASE_TYPE;

    private static final Clock fixedClock = Clock.fixed(Instant.parse(TIMESTAMP_AS_TEXT), ZoneOffset.UTC);
    private static final ZonedDateTime LOG_TIMESTAMP =
        ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);

    @TestConfiguration
    static class MockConfig {

        @Bean
        public AuthTokenGenerator authTokenGenerator() {
            return Mockito.mock(AuthTokenGenerator.class);
        }
    }

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        IdamUser user = new IdamUser();
        user.setId(IDAM_ID);
        doReturn(user).when(userRepository).getUser();

        auditService = new AuditService(fixedClock, userRepository, securityUtils, auditRepository,
            auditCaseRemoteConfiguration, auditCaseRemoteOperation);
    }

    @AfterEach
    public void after() throws IOException {
        WireMock.reset();
    }

    @Test
    public void shouldMakeAuditRequestWhenPerformingCaseSearch() throws JsonProcessingException, InterruptedException {

        final SearchLog searchLog = new SearchLog();
        searchLog.setUserId(SEARCH_LOG_USER_ID);
        searchLog.setCaseRefs(SEARCH_LOG_CASE_REFS);
        searchLog.setTimestamp(LOG_TIMESTAMP);

        CaseSearchPostRequest caseSearchPostRequest = new CaseSearchPostRequest(searchLog);

        stubFor(WireMock.post(urlMatching(SEARCH_AUDIT_ENDPOINT))
            .withHeader(SERVICE_AUTHORIZATION_HEADER, matching("Bearer .+"))
            .withRequestBody(equalToJson(objectMapper.writeValueAsString(caseSearchPostRequest)))
            .willReturn(okJson(objectMapper.writeValueAsString(caseSearchPostRequest))
                .withStatus(SEARCH_AUDIT_HTTP_STATUS)));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);

        AuditContext auditContext = AuditContext.auditContextWith()
                .caseId(CASE_ID)
                .auditOperationType(AuditOperationType.SEARCH_CASE)
                .jurisdiction(JURISDICTION)
                .caseType(CASE_TYPE)
                .httpStatus(200)
                .build();

        auditService.audit(auditContext);
        waitForPossibleAuditResponse(SEARCH_AUDIT_ENDPOINT, 1);

        Mockito.verify(auditCaseRemoteOperation).postCaseSearch(captor.capture(), ArgumentMatchers.any());
        assertThat(captor.getValue().getOperationType(), is(equalTo(AuditOperationType.SEARCH_CASE.getLabel())));
        assertThat(captor.getValue().getJurisdiction(), is(equalTo(JURISDICTION)));
        assertThat(captor.getValue().getCaseId(), is(equalTo(CASE_ID)));
        assertThat(captor.getValue().getCaseType(), is(equalTo(CASE_TYPE)));
        verifyWireMock(1, postRequestedFor(urlEqualTo(SEARCH_AUDIT_ENDPOINT))
            .withRequestBody(equalToJson(EXPECTED_CASE_SEARCH_LOG_JSON)));
    }

    @Test
    public void shouldMakeAuditRequestWhenPerformingCaseAction() throws JsonProcessingException, InterruptedException {
        AuditContext auditContext = AuditContext.auditContextWith()
            .caseId(CASE_ID)
            .auditOperationType(AuditOperationType.CASE_ACCESSED)
            .jurisdiction(JURISDICTION)
            .caseType(CASE_TYPE)
            .httpStatus(200)
            .build();

        CaseActionPostRequest caseActionPostRequest = new CaseActionPostRequest(
            new ActionLog(
                ACTION_LOG_USER_ID,
                ACTION_LOG_CASE_ACTION,
                ACTION_LOG_CASE_REF,
                ACTION_LOG_CASE_JURISDICTION_ID,
                ACTION_LOG_CASE_TYPE_ID,
                LOG_TIMESTAMP)
        );

        stubFor(WireMock.post(urlMatching(ACTION_AUDIT_ENDPOINT))
            .withHeader(SERVICE_AUTHORIZATION_HEADER, matching("Bearer .+"))
            .withRequestBody(equalToJson(objectMapper.writeValueAsString(caseActionPostRequest)))
            .willReturn(okJson(objectMapper.writeValueAsString(caseActionPostRequest))
                .withStatus(ACTION_AUDIT_HTTP_STATUS)));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);

        auditService.audit(auditContext);
        waitForPossibleAuditResponse(ACTION_AUDIT_ENDPOINT, 1);

        Mockito.verify(auditCaseRemoteOperation).postCaseAction(captor.capture(), ArgumentMatchers.any());
        assertThat(captor.getValue().getOperationType(), is(equalTo(AuditOperationType.CASE_ACCESSED.getLabel())));
        assertThat(captor.getValue().getJurisdiction(), is(equalTo(JURISDICTION)));
        assertThat(captor.getValue().getCaseId(), is(equalTo(CASE_ID)));
        assertThat(captor.getValue().getCaseType(), is(equalTo(CASE_TYPE)));
        verifyWireMock(1, postRequestedFor(urlEqualTo(ACTION_AUDIT_ENDPOINT))
            .withRequestBody(equalToJson(EXPECTED_CASE_ACTION_LOG_JSON)));
    }

    @Test
    public void shouldNotThrowExceptionInAuditServiceIfLauIsDown()
        throws JsonProcessingException, InterruptedException {
        AuditContext auditContext = AuditContext.auditContextWith()
            .caseId(CASE_ID)
            .auditOperationType(AuditOperationType.CASE_ACCESSED)
            .jurisdiction(JURISDICTION)
            .caseType(CASE_TYPE)
            .httpStatus(200)
            .build();

        CaseActionPostRequest caseActionPostRequest = new CaseActionPostRequest(
            new ActionLog(
                ACTION_LOG_USER_ID,
                ACTION_LOG_CASE_ACTION,
                ACTION_LOG_CASE_REF,
                ACTION_LOG_CASE_JURISDICTION_ID,
                ACTION_LOG_CASE_TYPE_ID,
                LOG_TIMESTAMP)
        );

        stubFor(WireMock.post(urlMatching(ACTION_AUDIT_ENDPOINT))
            .withHeader(SERVICE_AUTHORIZATION_HEADER, matching("Bearer .+"))
            .withRequestBody(equalToJson(objectMapper.writeValueAsString(caseActionPostRequest)))
            .willReturn(aResponse().withStatus(AUDIT_UNAUTHORISED_HTTP_STATUS)));

        auditService.audit(auditContext);
        waitForPossibleAuditResponse(ACTION_AUDIT_ENDPOINT, 3);

        verifyWireMock(3, postRequestedFor(urlEqualTo(ACTION_AUDIT_ENDPOINT))
            .withRequestBody(equalToJson(EXPECTED_CASE_ACTION_LOG_JSON)));
    }

    @Test
    public void shouldNotThrowExceptionInAuditServiceIfLauSearchIsDownAndRetry()
        throws JsonProcessingException, InterruptedException {
        final SearchLog searchLog = new SearchLog();
        searchLog.setUserId(SEARCH_LOG_USER_ID);
        searchLog.setCaseRefs(SEARCH_LOG_CASE_REFS);
        searchLog.setTimestamp(LOG_TIMESTAMP);

        CaseSearchPostRequest caseSearchPostRequest = new CaseSearchPostRequest(searchLog);

        stubFor(WireMock.post(urlMatching(SEARCH_AUDIT_ENDPOINT))
            .withHeader(SERVICE_AUTHORIZATION_HEADER, matching("Bearer .+"))
            .withRequestBody(equalToJson(objectMapper.writeValueAsString(caseSearchPostRequest)))
            .willReturn(aResponse().withStatus(AUDIT_UNAUTHORISED_HTTP_STATUS)));

        AuditContext auditContext = AuditContext.auditContextWith()
            .caseId(CASE_ID)
            .auditOperationType(AuditOperationType.SEARCH_CASE)
            .jurisdiction(JURISDICTION)
            .caseType(CASE_TYPE)
            .httpStatus(200)
            .build();

        auditService.audit(auditContext);
        waitForPossibleAuditResponse(SEARCH_AUDIT_ENDPOINT, 3);

        verifyWireMock(3, postRequestedFor(urlEqualTo(SEARCH_AUDIT_ENDPOINT))
            .withRequestBody(equalToJson(EXPECTED_CASE_SEARCH_LOG_JSON)));
    }

    public void shouldRetryIf403StatusFromLAU()
        throws JsonProcessingException, InterruptedException {
        AuditContext auditContext = AuditContext.auditContextWith()
            .caseId(CASE_ID)
            .auditOperationType(AuditOperationType.CASE_ACCESSED)
            .jurisdiction(JURISDICTION)
            .caseType(CASE_TYPE)
            .httpStatus(200)
            .build();

        CaseActionPostRequest caseActionPostRequest = new CaseActionPostRequest(
            new ActionLog(
                ACTION_LOG_USER_ID,
                ACTION_LOG_CASE_ACTION,
                ACTION_LOG_CASE_REF,
                ACTION_LOG_CASE_JURISDICTION_ID,
                ACTION_LOG_CASE_TYPE_ID,
                LOG_TIMESTAMP)
        );

        stubFor(WireMock.post(urlMatching(ACTION_AUDIT_ENDPOINT))
            .withHeader(SERVICE_AUTHORIZATION_HEADER, matching("Bearer .+"))
            .withRequestBody(equalToJson(objectMapper.writeValueAsString(caseActionPostRequest)))
            .willReturn(aResponse().withStatus(AUDIT_FORBIDDEN_HTTP_STATUS)));

        auditService.audit(auditContext);
        waitForPossibleAuditResponse(ACTION_AUDIT_ENDPOINT);

        verifyWireMock(3, postRequestedFor(urlEqualTo(ACTION_AUDIT_ENDPOINT))
            .withRequestBody(equalToJson(EXPECTED_CASE_ACTION_LOG_JSON)));
    }

    public void shouldRetryIfResponseStatus502()
        throws JsonProcessingException, InterruptedException {
        AuditContext auditContext = AuditContext.auditContextWith()
            .caseId(CASE_ID)
            .auditOperationType(AuditOperationType.CASE_ACCESSED)
            .jurisdiction(JURISDICTION)
            .caseType(CASE_TYPE)
            .httpStatus(200)
            .build();

        CaseActionPostRequest caseActionPostRequest = new CaseActionPostRequest(
            new ActionLog(
                ACTION_LOG_USER_ID,
                ACTION_LOG_CASE_ACTION,
                ACTION_LOG_CASE_REF,
                ACTION_LOG_CASE_JURISDICTION_ID,
                ACTION_LOG_CASE_TYPE_ID,
                LOG_TIMESTAMP)
        );

        stubFor(WireMock.post(urlMatching(ACTION_AUDIT_ENDPOINT))
            .withHeader(SERVICE_AUTHORIZATION_HEADER, matching("Bearer .+"))
            .withRequestBody(equalToJson(objectMapper.writeValueAsString(caseActionPostRequest)))
            .willReturn(aResponse().withStatus(AUDIT_BAD_GATEWAY_HTTP_STATUS)));

        auditService.audit(auditContext);
        waitForPossibleAuditResponse(ACTION_AUDIT_ENDPOINT);

        verifyWireMock(3, postRequestedFor(urlEqualTo(ACTION_AUDIT_ENDPOINT))
            .withRequestBody(equalToJson(EXPECTED_CASE_ACTION_LOG_JSON)));
    }

    public void shouldRetryIfResponseStatus504()
        throws JsonProcessingException, InterruptedException {
        AuditContext auditContext = AuditContext.auditContextWith()
            .caseId(CASE_ID)
            .auditOperationType(AuditOperationType.CASE_ACCESSED)
            .jurisdiction(JURISDICTION)
            .caseType(CASE_TYPE)
            .httpStatus(200)
            .build();

        CaseActionPostRequest caseActionPostRequest = new CaseActionPostRequest(
            new ActionLog(
                ACTION_LOG_USER_ID,
                ACTION_LOG_CASE_ACTION,
                ACTION_LOG_CASE_REF,
                ACTION_LOG_CASE_JURISDICTION_ID,
                ACTION_LOG_CASE_TYPE_ID,
                LOG_TIMESTAMP)
        );

        stubFor(WireMock.post(urlMatching(ACTION_AUDIT_ENDPOINT))
            .withHeader(SERVICE_AUTHORIZATION_HEADER, matching("Bearer .+"))
            .withRequestBody(equalToJson(objectMapper.writeValueAsString(caseActionPostRequest)))
            .willReturn(aResponse().withStatus(AUDIT_GATEWAY_TIMEOUT_HTTP_STATUS)));

        auditService.audit(auditContext);
        waitForPossibleAuditResponse(ACTION_AUDIT_ENDPOINT);

        verifyWireMock(3, postRequestedFor(urlEqualTo(ACTION_AUDIT_ENDPOINT))
            .withRequestBody(equalToJson(EXPECTED_CASE_ACTION_LOG_JSON)));
    }

    private void waitForPossibleAuditResponse(String pathPrefix) throws InterruptedException {
        waitForPossibleAuditResponse(pathPrefix, 1);
    }

    private void waitForPossibleAuditResponse(String pathPrefix, int expectedCount) throws InterruptedException {
        long finishTime = System.currentTimeMillis() + ASYNC_DELAY_TIMEOUT_MILLISECONDS;
        long currentCount = countServeEvents(pathPrefix);

        while (System.currentTimeMillis() < finishTime && currentCount < expectedCount) {
            TimeUnit.MILLISECONDS.sleep(ASYNC_DELAY_INTERVAL_MILLISECONDS);
            currentCount = countServeEvents(pathPrefix);
        }
    }

    private long countServeEvents(String pathPrefix) {
        return getAllServeEvents().stream()
            .filter(serveEvent -> serveEvent.getRequest().getUrl().startsWith(pathPrefix))
            .count();
    }

    @Test
    void shouldUseNewTokenOnRetryWithInterceptor() throws Exception {
        Mockito.when(authTokenGenerator.generate())
            .thenReturn("Bearer originalToken")
            .thenReturn("Bearer refreshedToken");

        stubFor(WireMock.post(urlEqualTo(ACTION_AUDIT_ENDPOINT))
            .willReturn(aResponse().withStatus(AUDIT_UNAUTHORISED_HTTP_STATUS)));

        AuditContext auditContext = AuditContext.auditContextWith()
            .caseId(CASE_ID)
            .auditOperationType(AuditOperationType.CASE_ACCESSED)
            .jurisdiction(JURISDICTION)
            .caseType(CASE_TYPE)
            .httpStatus(200)
            .build();

        // Act: make call (will retry 3 times)
        auditService.audit(auditContext);
        waitForPossibleAuditResponse(ACTION_AUDIT_ENDPOINT, 3);

        // Assert: all 3 requests
        var requests = getAllServeEvents().stream()
            .filter(e -> e.getRequest().getUrl().equals(ACTION_AUDIT_ENDPOINT))
            .toList();

        assertThat(requests.size(), is(3));

        var originalRequest = requests.stream()
            .filter(r -> r.getRequest().getHeader("ServiceAuthorization").equals("Bearer originalToken"))
            .toList();

        var retryRequests = requests.stream()
            .filter(r -> r.getRequest().getHeader("ServiceAuthorization").equals("Bearer refreshedToken"))
            .toList();

        assertThat(retryRequests.size(), is(2));
        assertThat(originalRequest.size(), is(1));
        assertThat(originalRequest.getFirst(), is(notNullValue()));
    }

}
