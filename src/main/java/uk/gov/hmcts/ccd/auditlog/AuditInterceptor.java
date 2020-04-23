package uk.gov.hmcts.ccd.auditlog;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import uk.gov.hmcts.ccd.auditlog.aop.AuditAspect;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

public class AuditInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(AuditAspect.class);

    public static final String REQUEST_ID = "request-id";

    private final AuditService auditService;

    private static final List<HttpStatus> IGNORED_STATUSES = Lists.newArrayList(NOT_FOUND);

    public AuditInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable Exception ex) {
        if (hasLogAudit(handler) && !IGNORED_STATUSES.contains(HttpStatus.resolve(response.getStatus()))) {
            AuditContext auditContext = AuditContextHolder.getAuditContext();
            auditContext = populateHttpSemantics(auditContext, request, response);
            try {
                auditService.audit(auditContext);
            } catch (Exception e) {  // Ignoring audit failures
                LOG.error("Error while auditing the request data:{}", e.getMessage());
            } finally {
                AuditContextHolder.remove();
            }
        }
    }

    private boolean hasLogAudit(Object handler) {
        return handler instanceof HandlerMethod && ((HandlerMethod) handler).hasMethodAnnotation(LogAudit.class);
    }

    private AuditContext populateHttpSemantics(AuditContext auditContext,
                                               HttpServletRequest request, HttpServletResponse response) {
        AuditContext context = (auditContext != null) ? auditContext : new AuditContext();
        context.setHttpStatus(response.getStatus());
        context.setHttpMethod(request.getMethod());
        context.setRequestPath(request.getRequestURI());
        context.setRequestId(request.getHeader(REQUEST_ID));
        return context;
    }
}
