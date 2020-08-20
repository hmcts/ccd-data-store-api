package uk.gov.hmcts.ccd.logging;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter
public class MdcFilter extends HttpFilter {

    @Autowired
    private CorrelationIDHttpExtractor correlationIDHttpExtractor;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws IOException, ServletException {
        try {
            MDC.put("CorrelationId", getCorrelationId(request));
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("CorrelationId");
        }
    }

    private String getCorrelationId(HttpServletRequest request) {
        return correlationIDHttpExtractor.getCorrelationID(request);
    }
}
