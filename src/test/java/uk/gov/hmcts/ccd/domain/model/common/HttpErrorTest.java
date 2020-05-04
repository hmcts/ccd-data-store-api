package uk.gov.hmcts.ccd.domain.model.common;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class HttpErrorTest {

    private static final String MESSAGE = "Error message";
    private static final String DETAILS = "Details of the error";
    private static final String PATH = "/some/rest/resource";

    private HttpServletRequest request;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        doReturn(PATH).when(request).getRequestURI();
    }

    @Test
    public void shouldExtractExceptionNameFromException() {
        final HttpError error = new HttpError(new IllegalArgumentException(), request);

        assertThat(error.getException(), is(equalTo("java.lang.IllegalArgumentException")));
    }

    @Test
    public void shouldExtractTimestampFromException() {
        final HttpError error = new HttpError(new IllegalArgumentException(), request);

        assertThat(error.getTimestamp(), is(notNullValue()));
    }

    @Test
    public void shouldExtractStatusFromExceptionAnnotation_withCode() {
        final HttpError error = new HttpError(new TestCodeStatusException(), request);

        assertThat(error.getStatus(), is(equalTo(415)));
    }

    @Test
    public void shouldExtractStatusFromExceptionAnnotation_withValue() {
        final HttpError error = new HttpError(new TestValueStatusException(), request);

        assertThat(error.getStatus(), is(equalTo(404)));
    }

    @Test
    public void shouldExtractStatusFromExceptionAnnotation_default() {
        final HttpError error = new HttpError(new NullPointerException(), request);

        assertThat(error.getStatus(), is(equalTo(HttpError.DEFAULT_STATUS)));
    }

    @Test
    public void shouldExtractErrorFromExceptionAnnotation_withReason() {
        final HttpError error = new HttpError(new TestReasonException(), request);

        assertThat(error.getError(), is(equalTo("Some error reason")));
    }

    @Test
    public void shouldExtractErrorFromExceptionAnnotation_withStatus() {
        final HttpError error = new HttpError(new TestCodeStatusException(), request);

        assertThat(error.getError(), is(equalTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())));
    }

    @Test
    public void shouldExtractErrorFromExceptionAnnotation_default() {
        final HttpError error = new HttpError(new IllegalArgumentException(), request);

        assertThat(error.getError(), is(equalTo(HttpError.DEFAULT_ERROR)));
    }

    @Test
    public void shouldExtractMessageFromException() {
        final HttpError error = new HttpError(new IllegalArgumentException(MESSAGE), request);

        assertThat(error.getMessage(), is(equalTo(MESSAGE)));
    }

    @Test
    public void shouldExtractPathFromRequest() {
        final HttpError error = new HttpError(new IllegalArgumentException(MESSAGE), request);

        assertThat(error.getPath(), is(equalTo(PATH)));
    }

    @Test
    public void shouldTakeOptionalDetails() {
        final HttpError<String> error = new HttpError<String>(new IllegalArgumentException(MESSAGE), request)
            .withDetails(DETAILS);

        assertThat(error.getDetails(), is(equalTo(DETAILS)));
    }

    @ResponseStatus(code = HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    class TestCodeStatusException extends RuntimeException {

    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    class TestValueStatusException extends RuntimeException {

    }

    @ResponseStatus(reason = "Some error reason")
    class TestReasonException extends RuntimeException {

    }

}
