package uk.gov.hmcts.ccd.domain.model.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.servlet.http.HttpServletRequest;


import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class HttpErrorTest {

    private static final String MESSAGE = "Error message";
    private static final String DETAILS = "Details of the error";
    private static final String PATH = "/some/rest/resource";

    private HttpServletRequest request;

    @BeforeEach
    public void setUp() {
        request = mock(HttpServletRequest.class);
        doReturn(PATH).when(request).getRequestURI();
    }

    @Test
    public void shouldExtractExceptionNameFromException() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new IllegalArgumentException(), request);

        assertThat(error.getException(), is(equalTo("Exception")));
    }

    @Test
    public void shouldExtractTimestampFromException() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new IllegalArgumentException(), request);

        assertThat(error.getTimestamp(), is(notNullValue()));
    }

    @Test
    public void shouldExtractStatusFromExceptionAnnotation_withCode() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new TestCodeStatusException(), request);

        assertThat(error.getStatus(), is(equalTo(415)));
    }

    @Test
    public void shouldExtractStatusFromExceptionAnnotation_withValue() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new TestValueStatusException(), request);

        assertThat(error.getStatus(), is(equalTo(404)));
    }

    @Test
    public void shouldExtractStatusFromExceptionAnnotation_default() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new NullPointerException(), request);

        assertThat(error.getStatus(), is(equalTo(uk.gov.hmcts.ccd.domain.model.common.HttpError.DEFAULT_STATUS)));
    }

    @Test
    public void shouldExtractErrorFromExceptionAnnotation_withReason() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new TestReasonException(), request);

        assertThat(error.getError(), is(equalTo("Internal Server Error")));
    }

    @Test
    public void shouldExtractErrorFromExceptionAnnotation_withStatus() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new TestCodeStatusException(), request);

        assertThat(error.getError(), is(equalTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())));
    }

    @Test
    public void shouldExtractErrorFromExceptionAnnotation_default() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new IllegalArgumentException(), request);

        assertThat(error.getError(), is(equalTo(uk.gov.hmcts.ccd.domain.model.common.HttpError.DEFAULT_ERROR)));
    }

    @Test
    public void shouldExtractMessageFromException() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new IllegalArgumentException(MESSAGE), request);

        assertThat(error.getMessage(), is(equalTo("Internal Server Error")));
    }

    @Test
    public void shouldExtractPathFromRequest() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new IllegalArgumentException(MESSAGE), request);

        assertThat(error.getPath(), is(equalTo("/")));
    }

    @Test
    public void shouldKeepSafePathForUntrustedRequestUri() {
        HttpServletRequest testRequest = mock(HttpServletRequest.class);
        doReturn("/private/path with spaces").when(testRequest).getRequestURI();
        final HttpError error = new HttpError(new IllegalArgumentException(MESSAGE), testRequest);
        assertThat(error.getPath(), is(equalTo("/")));
    }

    @Test
    public void shouldTakeOptionalDetails() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError<String> error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError<String>(new IllegalArgumentException(MESSAGE),
                        request)
            .withDetails(DETAILS);

        assertThat(error.getDetails(), is(equalTo(DETAILS)));
    }

    @Test
    public void shouldGetStatusFromPassedHttpStatus() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
            new uk.gov.hmcts.ccd.domain.model.common.HttpError(new NullPointerException(), PATH,
                HttpStatus.BAD_REQUEST);

        assertThat(error.getStatus(), is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void shouldReturnDefaultStatusWhenStatusAndDerivedStatusNull() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
            new uk.gov.hmcts.ccd.domain.model.common.HttpError(new NullPointerException(), PATH, null);

        assertThat(error.getStatus(), is(equalTo(uk.gov.hmcts.ccd.domain.model.common.HttpError.DEFAULT_STATUS)));
    }

    @Test
    public void shouldDeriveStatusFromResponseStatus() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
            new uk.gov.hmcts.ccd.domain.model.common.HttpError(new TestCodeStatusException(), PATH, null);

        assertThat(error.getStatus(), is(equalTo(415)));
    }

    @Test
    public void shouldReturnErrorFromResponseStatusReason() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
            new uk.gov.hmcts.ccd.domain.model.common.HttpError(new TestReasonException(), PATH, null);

        assertThat(error.getError(), is(equalTo("Internal Server Error")));
    }

    @Test
    public void shouldDeriveErrorFromResponseStatusHttpStatus() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
            new uk.gov.hmcts.ccd.domain.model.common.HttpError(new TestCodeStatusException(), PATH, null);

        assertThat(error.getError(), is(equalTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())));
    }

    @Test
    public void shouldReturnDefaultErrorWhenReasonPhraseIsNull() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
            new uk.gov.hmcts.ccd.domain.model.common.HttpError(new IllegalArgumentException(), PATH, null);

        assertThat(error.getError(), is(equalTo(uk.gov.hmcts.ccd.domain.model.common.HttpError.DEFAULT_ERROR)));
    }

    @Test
    public void shouldDeriveReasonPhraseFromPassedHttpStatus() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
            new uk.gov.hmcts.ccd.domain.model.common.HttpError(new NullPointerException(), PATH,
                HttpStatus.BAD_REQUEST);

        assertThat(error.getError(), is(equalTo(HttpStatus.BAD_REQUEST.getReasonPhrase())));
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
