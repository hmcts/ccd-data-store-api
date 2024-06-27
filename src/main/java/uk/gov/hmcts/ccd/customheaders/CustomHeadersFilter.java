package uk.gov.hmcts.ccd.customheaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService;
import uk.gov.hmcts.ccd.util.ClientContextUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomHeadersFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(CustomHeadersFilter.class);

    private final ApplicationParams applicationParams;

    public CustomHeadersFilter(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
        throws IOException, ServletException {
        LOG.info("doFilter called!");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        filterChain.doFilter(request, wrappedResponse);

        if (null != applicationParams
            && null != applicationParams.getCallbackPassthruHeaderContexts()) {
            applicationParams.getCallbackPassthruHeaderContexts().stream()
                .filter(StringUtils::hasLength)
                .forEach(context -> setContextHeader(context, httpRequest, wrappedResponse));
        }

        wrappedResponse.copyBodyToResponse();
    }

    public void setContextHeader(String context, HttpServletRequest request, HttpServletResponse response) {
        // Extract the custom header from the request
        String headerValue = null;
        if (null != request.getAttribute(context)) {
            headerValue = request.getAttribute(context).toString();
            LOG.debug("Add request ATTRIBUTE context <{}> value: <{}> to the response header",
                context, headerValue);
        } else if (null != request.getHeader(context)) {
            headerValue = request.getHeader(context);
            LOG.debug("Add request HEADER context <{}> value: <{}> to the response header",
                context, headerValue);
        }

        // if the header exists then add it to the response
        if (headerValue != null) {
            if (context.equalsIgnoreCase(CallbackService.CLIENT_CONTEXT)) {
                headerValue = ClientContextUtil.removeEnclosingSquareBrackets(headerValue);
            }

            response.setHeader(context, headerValue);
        }
    }
}
