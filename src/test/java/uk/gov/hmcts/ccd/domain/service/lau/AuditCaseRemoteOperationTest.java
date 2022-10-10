package uk.gov.hmcts.ccd.domain.service.lau;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.AuditCaseRemoteConfiguration;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.data.SecurityUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("audit log specific calls")
class AuditCaseRemoteOperationTest {

    private static final String IDAM_ID = "52e06ea8-b80f-41cf-b245-79775c87717a";
    private static final String TARGET_IDAM_ID = "target@mail.com";
    private static final String REQUEST_ID_VALUE = "30f14c6c1fc85cba12bfd093aa8f90e3";
    private static final String PATH = "/someUri";
    private static final String HTTP_METHOD = "POST";
    private static final String CASE_ID = "123456";
    private static final String MULTIPLE_CASE_IDS = "123456, 123457, 123458";
    private static final String JURISDICTION = "AUTOTEST1";
    private static final String CASE_TYPE = "CaseType1";
    private static final String EVENT_NAME = "CreateCase";
    private static final List<String> TARGET_CASE_ROLES = Arrays.asList("CaseRole1", "CaseRole2");

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private HttpClient httpClient;

    @Mock
    private AuditCaseRemoteConfiguration auditCaseRemoteConfiguration;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    ArgumentCaptor<HttpRequest> captor;

    private Clock fixedClock = Clock.fixed(Instant.parse("2018-08-19T16:02:42.01Z"), ZoneOffset.UTC);

    private AuditCaseRemoteOperation auditCaseRemoteOperation;

    private AuditContext baseAuditContext = AuditContext.auditContextWith()
        .caseId(CASE_ID)
        .auditOperationType(AuditOperationType.CREATE_CASE)
        .jurisdiction(JURISDICTION)
        .caseType(CASE_TYPE)
        .eventName(EVENT_NAME)
        .targetIdamId(TARGET_IDAM_ID)
        .targetCaseRoles(TARGET_CASE_ROLES)
        .httpMethod(HTTP_METHOD)
        .httpStatus(200)
        .requestPath(PATH)
        .requestId(REQUEST_ID_VALUE)
        .build();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        doReturn("Bearer 1234").when(securityUtils).getServiceAuthorization();
        doReturn("http://localhost/caseAction").when(auditCaseRemoteConfiguration).getCaseActionAuditUrl();
        doReturn("http://localhost/caseSearch").when(auditCaseRemoteConfiguration).getCaseSearchAuditUrl();
        doReturn("{}").when(objectMapper).writeValueAsString(any());
        auditCaseRemoteOperation = new AuditCaseRemoteOperation(securityUtils, httpClient, objectMapper,
            auditCaseRemoteConfiguration);
    }

    @Test
    @DisplayName("should post case action remote audit request")
    void shouldPostCaseActionRemoteAuditRequest() {

        ZonedDateTime fixedDateTime = ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);
        AuditEntry entry = createBaseAuditEntryData(fixedDateTime);

        // Setup as viewing a case
        entry.setOperationType(AuditOperationType.CASE_ACCESSED.getLabel());

        when(httpClient.sendAsync(any(HttpRequest.class),
            any(HttpResponse.BodyHandler.class))).thenReturn(new CompletableFuture<Void>());
        auditCaseRemoteOperation.postCaseAction(entry, fixedDateTime);

        verify(httpClient).sendAsync(captor.capture(),any());

        assertThat(captor.getValue().uri().getPath(), is(equalTo("/caseAction")));
        assertThat(captor.getValue().headers().map().size(), is(equalTo(3)));
        assertThat(captor.getValue().headers().map().get("ServiceAuthorization").get(0), is(equalTo("Bearer 1234")));
        assertThat(captor.getValue().headers().map().get("Content-Type").get(0), is(equalTo("application/json")));
        assertThat(captor.getValue().headers().map().get("Accept").get(0), is(equalTo("application/json")));

        HttpRequest.BodyPublisher bodyPublisher = captor.getValue().bodyPublisher().get();
        assertThat(bodyPublisher.contentLength(), is(equalTo(2L)));
    }

    @Test
    @DisplayName("should post case search remote audit request")
    void shouldPostCaseSearchRemoteAuditRequest() {

        ZonedDateTime fixedDateTime = ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);
        AuditEntry entry = createBaseAuditEntryData(fixedDateTime);

        // Setup as searching a case
        entry.setOperationType(AuditOperationType.SEARCH_CASE.getLabel());
        entry.setCaseId(MULTIPLE_CASE_IDS);

        when(httpClient.sendAsync(any(HttpRequest.class),
            any(HttpResponse.BodyHandler.class))).thenReturn(new CompletableFuture<Void>());
        auditCaseRemoteOperation.postCaseSearch(entry, fixedDateTime);

        verify(httpClient).sendAsync(captor.capture(),any());

        assertThat(captor.getValue().uri().getPath(), is(equalTo("/caseSearch")));
        assertThat(captor.getValue().headers().map().size(), is(equalTo(3)));
        assertThat(captor.getValue().headers().map().get("ServiceAuthorization").get(0), is(equalTo("Bearer 1234")));
        assertThat(captor.getValue().headers().map().get("Content-Type").get(0), is(equalTo("application/json")));
        assertThat(captor.getValue().headers().map().get("Accept").get(0), is(equalTo("application/json")));

        HttpRequest.BodyPublisher bodyPublisher = captor.getValue().bodyPublisher().get();
        assertThat(bodyPublisher.contentLength(), is(equalTo(2L)));
    }

    @Test
    @DisplayName("should not post case search remote audit request if operational type not search")
    void shouldNotPostCaseSearchRemoteAuditRequest() {

        ZonedDateTime fixedDateTime = ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);
        AuditEntry entry = createBaseAuditEntryData(fixedDateTime);

        // Setup as non search operational type.
        entry.setOperationType(AuditOperationType.GRANT_CASE_ACCESS.getLabel());

        auditCaseRemoteOperation.postCaseSearch(entry, fixedDateTime);

        verifyNoInteractions(httpClient);
    }

    private AuditEntry createBaseAuditEntryData(ZonedDateTime fixedDateTime) {
        AuditEntry entry = new AuditEntry();

        String formattedDate = fixedDateTime.format(ISO_LOCAL_DATE_TIME);
        entry.setDateTime(formattedDate);
        entry.setHttpStatus(baseAuditContext.getHttpStatus());
        entry.setHttpMethod(baseAuditContext.getHttpMethod());
        entry.setPath(baseAuditContext.getRequestPath());
        entry.setRequestId(baseAuditContext.getRequestId());
        entry.setInvokingService(securityUtils.getServiceName());
        entry.setIdamId(IDAM_ID);

        entry.setOperationType(baseAuditContext.getAuditOperationType() != null
            ? baseAuditContext.getAuditOperationType().getLabel() : null);
        entry.setJurisdiction(baseAuditContext.getJurisdiction());
        entry.setCaseId(baseAuditContext.getCaseId());
        entry.setCaseType(baseAuditContext.getCaseType());
        entry.setListOfCaseTypes(baseAuditContext.getCaseTypeIds());
        entry.setEventSelected(baseAuditContext.getEventName());
        entry.setTargetIdamId(baseAuditContext.getTargetIdamId());
        entry.setTargetCaseRoles(baseAuditContext.getTargetCaseRoles());

        return entry;
    }

}
