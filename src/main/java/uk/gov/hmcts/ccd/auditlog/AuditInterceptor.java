package uk.gov.hmcts.ccd.auditlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.auditlog.aop.AuditAspect;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class AuditInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(AuditAspect.class);

    public static final String REQUEST_ID = "request-id";
    public static final String REQUEST_ID_PATTERN = "^[|A-Za-z0-9+/=_.-]+$";
    protected static final List<String> httpMethodList =
        Arrays.asList("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE");
    public static final String BAD_VALUE_TOKEN = "<bad value not written to logs>";

    private final AuditService auditService;
    private final ApplicationParams applicationParams;

    public AuditInterceptor(AuditService auditService, ApplicationParams applicationParams) {
        this.auditService = auditService;
        this.applicationParams = applicationParams;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable Exception ex) {
        if (applicationParams.isAuditLogEnabled() && hasAuditAnnotation(handler)) {
            if (!applicationParams.getAuditLogIgnoreStatuses().contains(response.getStatus())) {
                AuditContext auditContext = AuditContextHolder.getAuditContext();
                auditContext = populateHttpSemantics(auditContext, request, response);
                try {
                    auditService.audit(auditContext);
                } catch (Exception e) {  // Ignoring audit failures
                    LOG.error("Error while auditing the request data:{}", e.getMessage());
                }
            }
            AuditContextHolder.remove();
        }
    }

    private boolean hasAuditAnnotation(Object handler) {
        return handler instanceof HandlerMethod && ((HandlerMethod) handler).hasMethodAnnotation(LogAudit.class);
    }

    private AuditContext populateHttpSemantics(AuditContext auditContext,
                                               HttpServletRequest request, HttpServletResponse response) {
        AuditContext context = (auditContext != null) ? auditContext : new AuditContext();
        context.setHttpStatus(response.getStatus());

        String requestMethod = request.getMethod();
        if (requestMethod != null && httpMethodList.contains(requestMethod.toUpperCase())) {
            context.setHttpMethod(requestMethod);
        } else {
            context.setHttpMethod(BAD_VALUE_TOKEN);
            LOG.error("Error while validating Http Method with value {} as it did not meet validation criteria:{}",
                encodeString(requestMethod), httpMethodList);
        }

        context.setRequestPath(request.getRequestURI());

        String requestId = request.getHeader(REQUEST_ID);
        if (requestId == null || requestId.matches(REQUEST_ID_PATTERN)) {
            context.setRequestId(requestId);
        } else {
            context.setRequestId(BAD_VALUE_TOKEN);
            LOG.error("Error while validating Request Id with value {} as it did not meet validation criteria:{}",
                encodeString(requestId), REQUEST_ID_PATTERN);
        }
        return context;
    }

    private String encodeString(String value) {
        if (value == null) {
            value = "";
        }

        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
