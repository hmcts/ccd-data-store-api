package uk.gov.hmcts.ccd.auditlog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContext;
import uk.gov.hmcts.ccd.auditlog.aop.AuditContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LogAuditInterceptor extends HandlerInterceptorAdapter {
    private final Log logger = LogFactory.getLog(getClass());

    private final AuditService auditService;

    public LogAuditInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable ModelAndView modelAndView) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            logger.info("handlerMethod" + handlerMethod.getMethod().toString());
            if (handlerMethod.hasMethodAnnotation(LogAudit.class)) {
                AuditContext auditContext = AuditContextHolder.getAuditContext();
                logger.info(auditService.prepareAuditMessage(request, response.getStatus(), auditContext));
                AuditContextHolder.remove();
            }
        }
    }

}
