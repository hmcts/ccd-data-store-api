package uk.gov.hmcts.ccd.auditlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

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

    private Clock fixedClock = Clock.fixed(Instant.parse("2018-08-19T16:02:42.01Z"), ZoneOffset.UTC);

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        auditService = new AuditService(fixedClock, userRepository, securityUtils);
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
            given(response.getContentAsByteArray()).willReturn("".getBytes());

            String message = auditService.prepareAuditMessage(request, response, OperationType.CREATE_CASE.getLabel());

            assertAll(
                () -> assertThat(message).contains("idamId=" + "'" + EMAIL + "'"),
                () -> assertThat(message).contains("invokingService=" + "'" + SERVICE_NAME + "'")
            );
        }

    }
}
