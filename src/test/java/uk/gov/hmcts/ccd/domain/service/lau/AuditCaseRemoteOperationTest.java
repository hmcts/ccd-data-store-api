package uk.gov.hmcts.ccd.domain.service.lau;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.AuditCaseRemoteConfiguration;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAndAuditFeignClient;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.lau.CaseActionPostRequest;
import uk.gov.hmcts.ccd.domain.model.lau.CaseSearchPostRequest;

import java.net.http.HttpRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.ccd.domain.service.lau.AuditCaseRemoteOperation.CASE_ACTION_MAP;

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
    private LogAndAuditFeignClient feignClient;

    @Mock
    private AuditCaseRemoteConfiguration auditCaseRemoteConfiguration;

    @Captor
    ArgumentCaptor<HttpRequest> captor;

    private Clock fixedClock = Clock.fixed(Instant.parse("2018-08-19T16:02:42.01Z"), ZoneOffset.UTC);

    @InjectMocks
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
        auditCaseRemoteOperation = new AuditCaseRemoteOperation(securityUtils, feignClient,
            auditCaseRemoteConfiguration);
    }

    @Test
    @DisplayName("should post case action remote audit request")
    void shouldPostCaseActionRemoteAuditRequest() {

        ZonedDateTime fixedDateTime = ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);
        AuditEntry entry = createBaseAuditEntryData(fixedDateTime);

        // Setup as viewing a case
        entry.setOperationType(AuditOperationType.CASE_ACCESSED.getLabel());

        auditCaseRemoteOperation.postCaseAction(entry, fixedDateTime);

        // Verify FeignClient interaction
        ArgumentCaptor<CaseActionPostRequest> requestCaptor = ArgumentCaptor.forClass(CaseActionPostRequest.class);
        verify(feignClient).postCaseAction(any(String.class), requestCaptor.capture());
        // Verify headers and endpoint
        verify(feignClient).postCaseAction(eq("Bearer 1234"), any(CaseActionPostRequest.class));
        assertThat(auditCaseRemoteConfiguration.getCaseActionAuditUrl(), is(equalTo("http://localhost/caseAction")));

        // Assert the captured request
        CaseActionPostRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getActionLog().getUserId(), is(equalTo(IDAM_ID)));
        assertThat(capturedRequest.getActionLog().getCaseAction(), is(equalTo(CASE_ACTION_MAP
            .get(AuditOperationType.CASE_ACCESSED.getLabel()))));
        assertThat(capturedRequest.getActionLog().getCaseJurisdictionId(), is(equalTo(JURISDICTION)));
        assertThat(capturedRequest.getActionLog().getCaseRef(), is(equalTo(CASE_ID)));
        assertThat(capturedRequest.getActionLog().getCaseTypeId(), is(equalTo(CASE_TYPE)));
        assertThat(capturedRequest.getActionLog().getTimestamp(), is(equalTo(fixedDateTime.toString())));

    }

    @Test
    @DisplayName("should post case search remote audit request")
    void shouldPostCaseSearchRemoteAuditRequest() {

        ZonedDateTime fixedDateTime = ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);
        AuditEntry entry = createBaseAuditEntryData(fixedDateTime);

        // Setup as searching a case
        entry.setOperationType(AuditOperationType.SEARCH_CASE.getLabel());
        entry.setCaseId(MULTIPLE_CASE_IDS);

        auditCaseRemoteOperation.postCaseSearch(entry, fixedDateTime);

        // Verify FeignClient interaction
        ArgumentCaptor<CaseSearchPostRequest> requestCaptor = ArgumentCaptor.forClass(CaseSearchPostRequest.class);
        verify(feignClient).postCaseSearch(any(String.class), requestCaptor.capture());
        verify(feignClient).postCaseSearch(eq("Bearer 1234"), any(CaseSearchPostRequest.class));
        assertThat(auditCaseRemoteConfiguration.getCaseSearchAuditUrl(), is(equalTo("http://localhost/caseSearch")));

        // Assert the captured request
        CaseSearchPostRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getSearchLog().getUserId(), is(equalTo(IDAM_ID)));
        assertThat(capturedRequest.getSearchLog().getCaseRefs(), is(equalTo(List.of(MULTIPLE_CASE_IDS.split(", ")))));
        assertThat(capturedRequest.getSearchLog().getTimestamp(), is(equalTo(fixedDateTime.toString())));
    }

    @Test
    @DisplayName("should not post case search remote audit request if operational type not search")
    void shouldNotPostCaseSearchRemoteAuditRequest() {

        ZonedDateTime fixedDateTime = ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);
        AuditEntry entry = createBaseAuditEntryData(fixedDateTime);

        // Setup as non search operational type.
        entry.setOperationType(AuditOperationType.GRANT_CASE_ACCESS.getLabel());

        auditCaseRemoteOperation.postCaseSearch(entry, fixedDateTime);

        verifyNoInteractions(feignClient);
    }

    @Test
    @DisplayName("should handle exception during postCaseAction")
    void shouldHandleExceptionDuringPostCaseAction() {

        ZonedDateTime fixedDateTime = ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);
        AuditEntry entry = createBaseAuditEntryData(fixedDateTime);

        // Setup as viewing a case
        entry.setOperationType(AuditOperationType.CASE_ACCESSED.getLabel());

        // Simulate exception in FeignClient
        doThrow(new RuntimeException("FeignClient error")).when(feignClient)
            .postCaseAction(any(String.class), any(CaseActionPostRequest.class));

        auditCaseRemoteOperation.postCaseAction(entry, fixedDateTime);

        // Verify exception is logged and no further interaction occurs
        verify(feignClient).postCaseAction(any(String.class), any(CaseActionPostRequest.class));
    }

    @Test
    @DisplayName("should handle exception during postSearchAction")
    void shouldHandleExceptionDuringPostSearchAction() {

        ZonedDateTime fixedDateTime = ZonedDateTime.of(LocalDateTime.now(fixedClock), ZoneOffset.UTC);
        AuditEntry entry = createBaseAuditEntryData(fixedDateTime);

        // Setup as viewing a case
        entry.setOperationType(AuditOperationType.SEARCH_CASE.getLabel());

        // Simulate exception in FeignClient
        doThrow(new RuntimeException("FeignClient error")).when(feignClient)
            .postCaseSearch(any(String.class), any(CaseSearchPostRequest.class));

        auditCaseRemoteOperation.postCaseSearch(entry, fixedDateTime);

        // Verify exception is logged and no further interaction occurs
        verify(feignClient).postCaseSearch(any(String.class), any(CaseSearchPostRequest.class));
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
