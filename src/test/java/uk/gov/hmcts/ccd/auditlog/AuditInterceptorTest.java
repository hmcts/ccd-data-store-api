package uk.gov.hmcts.ccd.auditlog;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

class AuditInterceptorTest {

    private static final int STATUS = 200;
    private static final String METHOD = "GET";
    private static final String REQUEST_URI = "/cases/1234";
    private static final String REQUEST_ID = "tes_request_id";

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private AuditInterceptor interceptor;
    @Mock
    private AuditService auditService;
    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private HandlerMethod handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        interceptor = new AuditInterceptor(auditService, applicationParams);

        request = new MockHttpServletRequest(METHOD, REQUEST_URI);
        request.addHeader(AuditInterceptor.REQUEST_ID, REQUEST_ID);
        response = new MockHttpServletResponse();
        response.setStatus(STATUS);

        given(applicationParams.isAuditLogEnabled()).willReturn(true);
        given(applicationParams.getAuditLogIgnoreStatuses()).willReturn(Lists.newArrayList(404));
    }

    @Test
    void shouldPrepareAuditContextWithHttpSemantics() {
        AuditContext auditContext = new AuditContext();

        given(handler.hasMethodAnnotation(LogAudit.class)).willReturn(true);
        AuditContextHolder.setAuditContext(auditContext);

        interceptor.afterCompletion(request, response, handler, null);

        assertThat(auditContext.getHttpMethod()).isEqualTo(METHOD);
        assertThat(auditContext.getRequestPath()).isEqualTo(REQUEST_URI);
        assertThat(auditContext.getHttpStatus()).isEqualTo(STATUS);
        assertThat(auditContext.getRequestId()).isEqualTo(REQUEST_ID);

        assertThat(AuditContextHolder.getAuditContext()).isNull();

        verify(auditService).audit(auditContext);
    }

    @Test
    void shouldNotAuditForWhenAnnotationIsNotPresent() {

        given(handler.hasMethodAnnotation(LogAudit.class)).willReturn(false);

        interceptor.afterCompletion(request, response, handler, null);

        verifyZeroInteractions(auditService);

    }

    @Test
    void shouldNotAuditFor404Status() {
        response.setStatus(404);
        given(handler.hasMethodAnnotation(LogAudit.class)).willReturn(true);

        interceptor.afterCompletion(request, response, handler, null);

        verifyZeroInteractions(auditService);

    }

    @Test
    void shouldClearAuditContextAlways() {

        AuditContext auditContext = new AuditContext();
        AuditContextHolder.setAuditContext(auditContext);

        given(handler.hasMethodAnnotation(LogAudit.class)).willReturn(true);
        doThrow(new RuntimeException("audit failure")).when(auditService).audit(auditContext);

        interceptor.afterCompletion(request, response, handler, null);

        assertThat(AuditContextHolder.getAuditContext()).isNull();
    }

    @Test
    void shouldNotAuditIfDisabled() {

        given(applicationParams.isAuditLogEnabled()).willReturn(false);

        interceptor.afterCompletion(request, response, handler, null);

        verifyZeroInteractions(auditService);

    }
}
