package uk.gov.hmcts.ccd.customheaders;

import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
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

    private final ApplicationParams applicationParams;

    public CustomHeadersFilter(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
        throws IOException, ServletException {
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
        } else if (null != request.getHeader(context)) {
            headerValue = request.getHeader(context);
        }

        // if the header exists then add it to the response
        if (headerValue != null) {
            if (context.equalsIgnoreCase(CallbackService.CLIENT_CONTEXT)) {
                headerValue = ClientContextUtil.removeEnclosingSquareBrackets(headerValue);
            }

            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            response.setHeader(context, headerValue);
            try {
                wrappedResponse.copyBodyToResponse();
            } catch (IOException e) {
                throw new ServiceException("Unable to copy cache to HttpResponse", e);
            }
        }
    }
}
