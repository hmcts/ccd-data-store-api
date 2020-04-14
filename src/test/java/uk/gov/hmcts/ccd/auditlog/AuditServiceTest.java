package uk.gov.hmcts.ccd.auditlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@DisplayName("audit log specific calls")
class AuditServiceTest {

    private static final String EMAIL = "ssss@mail.com";
    public static final String SERVICE_NAME = "ccd_api_gateway";

    @Mock
    private ContentCachingRequestWrapper request;

    @Mock
    private ContentCachingResponseWrapper response;

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

    @Nested
    @DisplayName("GET /internal/case-types/{caseTypeId}/work-basket-inputs")
    class GetWorkbasketInputsDetails {

        @Test
        @DisplayName("should return 200 when case found")
        void caseFound() {
            AuditContext auditContext = AuditContext.auditContextWith()
                .caseId("123456")
                .operationType(OperationType.CREATE_CASE)
                .jurisdiction("AUTOTEST1")
                .build();

            auditService.audit(request, 200, auditContext);

            verify(auditRepository).save(captor.capture());

            assertThat(captor.getValue().getIdamId()).isEqualTo((EMAIL));

        }

    }
}
