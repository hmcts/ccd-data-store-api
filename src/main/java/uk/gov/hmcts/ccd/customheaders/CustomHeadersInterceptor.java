package uk.gov.hmcts.ccd.customheaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.ccd.ApplicationParams;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class CustomHeadersInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(CustomHeadersInterceptor.class);
    public static final String HDR_CLIENT_CONTEXT = "Client-Context";

    private final ApplicationParams applicationParams;

    public CustomHeadersInterceptor(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        if (null != applicationParams
            && null != applicationParams.getCallbackPassthruHeaderContexts()) {
            applicationParams.getCallbackPassthruHeaderContexts().stream()
                .filter(StringUtils::hasLength)
                .forEach(context -> setContextHeader(context, request, response));
        }
    }

    private void setContextHeader(String context, HttpServletRequest request, HttpServletResponse response) {
        // Extract the custom header from the request
        String headerValue = null;
        if (null != request.getAttribute(context)) {
            headerValue = request.getAttribute(context).toString();
            LOG.debug("Add request ATTRIBUTE context <{}> value: <{}> to the response header",
                context, headerValue);
        } else if (null != request.getHeader(context)) {
            LOG.debug("Add request HEADER context <{}> value: <{}> to the response header",
                context, headerValue);
            headerValue = request.getHeader(context);
        }

        // if the header exists then add it to the response
        if (headerValue != null) {
            response.setHeader(context, headerValue);
        }
    }

}
