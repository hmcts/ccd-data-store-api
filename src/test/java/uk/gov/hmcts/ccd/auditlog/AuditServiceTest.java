package uk.gov.hmcts.ccd.auditlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

@DisplayName("audit log specific calls")
class AuditServiceTest {

    @Mock
    private ContentCachingRequestWrapper request;

    @Mock
    private ContentCachingResponseWrapper response;

    @Mock
    private HandlerExecutionChain handler;

    private Clock fixedClock = Clock.fixed(Instant.parse("2018-08-19T16:02:42.01Z"), ZoneOffset.UTC);

    private AuditService auditService = new AuditService(fixedClock);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Nested
    @DisplayName("GET /internal/case-types/{caseTypeId}/work-basket-inputs")
    class GetWorkbasketInputsDetails {

        @Test
        @DisplayName("should return 200 when case found")
        void caseFound() {
            given(response.getContentAsByteArray()).willReturn("".getBytes());

            String message = auditService.prepareAuditMessage(request, response, handler);

            assertAll(
                () -> assertEquals("CLA-CCD " + LocalDateTime.now(fixedClock).format(ISO_LOCAL_DATE_TIME), message)
            );
        }

    }
}
