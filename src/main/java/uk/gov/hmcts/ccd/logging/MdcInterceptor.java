package uk.gov.hmcts.ccd.logging;

import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MdcInterceptor implements HandlerInterceptor {

    private CorrelationIDExtractor correlationIDExtractor;
    private final static String CORRELATION_ID ="CorrelationId";

    public MdcInterceptor(CorrelationIDExtractor correlationIDHttpExtractor) {
        this.correlationIDExtractor = correlationIDHttpExtractor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        MDC.put(CORRELATION_ID, getCorrelationId(request));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.remove(CORRELATION_ID);
    }

    private String getCorrelationId(HttpServletRequest request) {
        return correlationIDExtractor.getCorrelationID(request);
    }
}
