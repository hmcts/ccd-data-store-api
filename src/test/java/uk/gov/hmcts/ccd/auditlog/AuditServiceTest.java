package uk.gov.hmcts.ccd.auditlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@DisplayName("audit log specific calls")
class AuditServiceTest {

    private static final String EMAIL = "ssss@mail.com";
    private static final String TARGET_IDAM_ID = "target@mail.com";
    private static final String SERVICE_NAME = "ccd_api_gateway";
    private static final String REQUEST_ID_VALUE = "30f14c6c1fc85cba12bfd093aa8f90e3";
    private static final String PATH = "/someUri";
    private static final String HTTP_METHOD = "POST";
    private static final String CASE_ID = "123456";
    private static final String JURISDICTION = "AUTOTEST1";
    private static final String CASE_TYPE = "CaseType1";
    private static final String EVENT_NAME = "CreateCase";
    private static final List<String> TARGET_CASE_ROLES = Arrays.asList("CaseRole1", "CaseRole2");

    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditRepository auditRepository;

    @Captor
    ArgumentCaptor<AuditEntry> captor;

    private Clock fixedClock = Clock.fixed(Instant.parse("2018-08-19T16:02:42.01Z"), ZoneOffset.UTC);

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        auditService = new AuditService(fixedClock, userRepository, securityUtils, auditRepository);
        IdamUser user = new IdamUser();
        user.setEmail(EMAIL);

        doReturn(user).when(userRepository).getUser();
        doReturn(SERVICE_NAME).when(securityUtils).getServiceName();
    }

    @Test
    @DisplayName("should save to audit repository")
    void shouldSaveToAuditRepository() {
        AuditContext auditContext = AuditContext.auditContextWith()
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

        auditService.audit(auditContext);

        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getDateTime(), is(equalTo("2018-08-19T16:02:42.01")));
        assertThat(captor.getValue().getHttpStatus(), is(equalTo(200)));
        assertThat(captor.getValue().getHttpMethod(), is(equalTo(HTTP_METHOD)));
        assertThat(captor.getValue().getPath(), is(equalTo((PATH))));
        assertThat(captor.getValue().getIdamId(), is(equalTo((EMAIL))));
        assertThat(captor.getValue().getInvokingService(), is(equalTo((SERVICE_NAME))));
        assertThat(captor.getValue().getRequestId(), is(equalTo((REQUEST_ID_VALUE))));

        assertThat(captor.getValue().getOperationType(), is(equalTo(AuditOperationType.CREATE_CASE.getLabel())));
        assertThat(captor.getValue().getJurisdiction(), is(equalTo(JURISDICTION)));
        assertThat(captor.getValue().getCaseId(), is(equalTo(CASE_ID)));
        assertThat(captor.getValue().getCaseType(), is(equalTo(CASE_TYPE)));
        assertThat(captor.getValue().getEventSelected(), is(equalTo(EVENT_NAME)));
        assertThat(captor.getValue().getTargetIdamId(), is(equalTo(TARGET_IDAM_ID)));
        assertThat(captor.getValue().getTargetCaseRoles().size(), is(equalTo(2)));
        assertTrue(captor.getValue().getTargetCaseRoles().contains("CaseRole1"));
        assertTrue(captor.getValue().getTargetCaseRoles().contains("CaseRole2"));
    }

    @Test
    @DisplayName("should save to audit repository")
    void shouldSaveToAuditRepositoryWithNullOperationType() {
        AuditContext auditContext = AuditContext.auditContextWith()
            .auditOperationType(null)
            .httpStatus(403)
            .build();

        auditService.audit(auditContext);

        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getHttpStatus(), is(equalTo(403)));
        assertThat(captor.getValue().getOperationType(), is(equalTo(null)));
    }
}
