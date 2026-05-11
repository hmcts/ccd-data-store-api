package uk.gov.hmcts.ccd.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.ccd.domain.model.common.HttpError;

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
}
