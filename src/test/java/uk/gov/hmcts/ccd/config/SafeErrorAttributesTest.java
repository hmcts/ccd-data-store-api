package uk.gov.hmcts.ccd.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import uk.gov.hmcts.ccd.domain.model.common.HttpError;

import jakarta.servlet.RequestDispatcher;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class SafeErrorAttributesTest {

    private final SafeErrorAttributes safeErrorAttributes = new SafeErrorAttributes();

    @Test
    void shouldUseAccessDeniedForForbiddenResponses() {
        assertThat(safeErrorAttributes.safeMessage(HttpStatus.FORBIDDEN.value()), is("Access Denied"));
    }

    @Test
    void shouldUseReasonPhraseForNonForbiddenResponses() {
        assertThat(safeErrorAttributes.safeMessage(HttpStatus.BAD_REQUEST.value()), is("Bad Request"));
    }

    @Test
    void shouldUseDefaultErrorWhenStatusCannotBeResolved() {
        assertThat(safeErrorAttributes.safeMessage(null), is(HttpError.DEFAULT_ERROR));
    }

    @Test
    void shouldOverrideDefaultErrorAttributesWithSafeMessageAndRemoveException() {
        Map<String, Object> errorAttributes = safeErrorAttributes.getErrorAttributes(
            errorRequest(HttpStatus.BAD_REQUEST, "Raw exception message"),
            ErrorAttributeOptions.defaults().including(
                ErrorAttributeOptions.Include.MESSAGE,
                ErrorAttributeOptions.Include.EXCEPTION
            )
        );

        assertThat(errorAttributes.get("message"), is("Bad Request"));
        assertThat(errorAttributes.containsKey("exception"), is(false));
    }

    private ServletWebRequest errorRequest(HttpStatus status, String message) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/some/path");
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status.value());
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, message);
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new IllegalArgumentException(message));
        return new ServletWebRequest(request);
    }
}
