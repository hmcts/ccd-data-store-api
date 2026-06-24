package uk.gov.hmcts.ccd.config;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;
import uk.gov.hmcts.ccd.domain.model.common.HttpError;

import java.util.Map;

@Component
public class SafeErrorAttributes extends DefaultErrorAttributes {

    private static final String ACCESS_DENIED_MESSAGE = "Access Denied";

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
        errorAttributes.put("message", safeMessage(errorAttributes.get("status")));
        errorAttributes.remove("exception");
        return errorAttributes;
    }

    String safeMessage(Object status) {
        if (status instanceof Number statusCode) {
            HttpStatus httpStatus = HttpStatus.resolve(statusCode.intValue());
            if (HttpStatus.FORBIDDEN.equals(httpStatus)) {
                return ACCESS_DENIED_MESSAGE;
            }
            if (httpStatus != null) {
                return httpStatus.getReasonPhrase();
            }
        }

        return HttpError.DEFAULT_ERROR;
    }
}
