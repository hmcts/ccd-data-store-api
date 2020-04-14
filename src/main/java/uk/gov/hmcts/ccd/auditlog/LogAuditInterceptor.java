package uk.gov.hmcts.ccd.auditlog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
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
        logger.info("postHandle invoked");
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            logger.info("handlerMethod" + handlerMethod.getMethod().toString());
            if (handlerMethod.hasMethodAnnotation(LogAudit.class)) {
                logger.info("hasMethodAnnotation: " + handlerMethod.hasMethodAnnotation(LogAudit.class));
                if (!(request instanceof ContentCachingRequestWrapper)) {
                    request = new ContentCachingRequestWrapper(request);
                }
                if (!(response instanceof ContentCachingResponseWrapper)) {
                    response = new ContentCachingResponseWrapper(response);
                }

                LogAudit logAudit = handlerMethod.getMethodAnnotation(LogAudit.class);

                readValuesSetByAudiAspect();

                String operationType = logAudit.operationType().getLabel();
                logger.info(auditService.prepareAuditMessage(request, response, operationType));

            }
        }
    }

    private void readValuesSetByAudiAspect() {
        // read values from thread local context and call clear in the finally block.
        LogMessage context = AuditContextHolder.getAuditContext();
        if (context != null) {
            logger.info("CaseId from context:" + context.getCaseId());
            logger.info("Jurisdiction from context:" + context.getJurisdiction());
        }
    }
}
