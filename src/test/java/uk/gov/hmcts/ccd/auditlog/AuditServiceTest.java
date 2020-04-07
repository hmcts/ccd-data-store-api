package uk.gov.hmcts.ccd.auditlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.time.LocalDateTime;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("audit log specific calls")
class AuditServiceTest {

    @Mock
    private ContentCachingRequestWrapper request;

    @Mock
    private ContentCachingResponseWrapper response;

    @Mock
    private HandlerExecutionChain handler;

    private AuditService auditService = new AuditService();

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
            String now = LocalDateTime.now().format(ISO_LOCAL_DATE_TIME);
            BDDMockito.given(response.getContentAsByteArray()).willReturn("".getBytes());


            String message = auditService.prepareAuditMessage(request, response, handler, now);

            assertAll(
                () -> assertEquals("CLA-CCD " + now, message)
            );
        }

    }
}
