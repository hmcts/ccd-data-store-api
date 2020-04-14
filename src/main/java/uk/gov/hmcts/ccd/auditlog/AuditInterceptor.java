package uk.gov.hmcts.ccd.auditlog;

import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuditInterceptor extends HandlerInterceptorAdapter {

    private final AuditService auditService;

    public AuditInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable ModelAndView modelAndView) {
        if (handler instanceof HandlerMethod && ((HandlerMethod) handler).hasMethodAnnotation(LogAudit.class)) {
            AuditContext auditContext = AuditContextHolder.getAuditContext();
            auditService.audit(request, response.getStatus(), auditContext);
            AuditContextHolder.remove();
        }
    }

}
