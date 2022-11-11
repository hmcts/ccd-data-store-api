package uk.gov.hmcts.ccd.domain.service.lau;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
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

import javax.inject.Inject;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
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
import static org.mockito.Mockito.doReturn;

public class AuditCaseRemoteOperationIT extends WireMockBaseTest {

    private static int ASYNC_DELAY_TIMEOUT_MILLISECONDS = 2000;
    private static int ASYNC_DELAY_INTERVAL_MILLISECONDS = 200;

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
    private SecurityUtils securityUtils;

    @Autowired
    private AuditCaseRemoteConfiguration auditCaseRemoteConfiguration;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditRepository auditRepository;

    @SpyBean
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
    private static final int AUDIT_NOT_FOUND_HTTP_STATUS = 404;

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

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        IdamUser user = new IdamUser();
        user.setId(IDAM_ID);
        doReturn(user).when(userRepository).getUser();

        auditService = new AuditService(fixedClock, userRepository, securityUtils, auditRepository,
            auditCaseRemoteConfiguration, auditCaseRemoteOperation);
    }

    @After
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
        waitForPossibleAuditResponse(SEARCH_AUDIT_ENDPOINT);

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
        waitForPossibleAuditResponse(ACTION_AUDIT_ENDPOINT);

        Mockito.verify(auditCaseRemoteOperation).postCaseAction(captor.capture(), ArgumentMatchers.any());
        assertThat(captor.getValue().getOperationType(), is(equalTo(AuditOperationType.CASE_ACCESSED.getLabel())));
        assertThat(captor.getValue().getJurisdiction(), is(equalTo(JURISDICTION)));
        assertThat(captor.getValue().getCaseId(), is(equalTo(CASE_ID)));
        assertThat(captor.getValue().getCaseType(), is(equalTo(CASE_TYPE)));
        verifyWireMock(1, postRequestedFor(urlEqualTo(ACTION_AUDIT_ENDPOINT))
            .withRequestBody(equalToJson(EXPECTED_CASE_ACTION_LOG_JSON)));
    }

    @Test(expected = Test.None.class)
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
            .willReturn(aResponse().withStatus(AUDIT_NOT_FOUND_HTTP_STATUS)));

        auditService.audit(auditContext);
        waitForPossibleAuditResponse(ACTION_AUDIT_ENDPOINT);

        verifyWireMock(1, postRequestedFor(urlEqualTo(ACTION_AUDIT_ENDPOINT))
            .withRequestBody(equalToJson(EXPECTED_CASE_ACTION_LOG_JSON)));
    }

    private void waitForPossibleAuditResponse(String pathPrefix) throws InterruptedException {
        List<ServeEvent> allServeEvents;
        boolean found = false;
        long finishTime = ZonedDateTime.now().toInstant().toEpochMilli() + ASYNC_DELAY_TIMEOUT_MILLISECONDS;

        while (ZonedDateTime.now().toInstant().toEpochMilli() < finishTime && !found) {
            allServeEvents = getAllServeEvents();
            for (ServeEvent serveEvent : allServeEvents) {
                if (serveEvent.getRequest().getUrl().startsWith(pathPrefix)) {
                    found = true;
                }
            }
            if (!found) {
                TimeUnit.MILLISECONDS.sleep(ASYNC_DELAY_INTERVAL_MILLISECONDS);
            }
        }
    }

}
