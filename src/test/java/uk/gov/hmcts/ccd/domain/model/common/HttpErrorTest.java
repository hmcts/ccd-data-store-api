package uk.gov.hmcts.ccd.domain.model.common;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
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
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new IllegalArgumentException(), request);

        assertThat(error.getException(), is(equalTo("java.lang.IllegalArgumentException")));
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

        assertThat(error.getError(), is(equalTo("Some error reason")));
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

        assertThat(error.getMessage(), is(equalTo(MESSAGE)));
    }

    @Test
    public void shouldExtractPathFromRequest() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new IllegalArgumentException(MESSAGE), request);

        assertThat(error.getPath(), is(equalTo(PATH)));
    }

    @Test
    public void shouldExtractPathFromRequest_useEncoding() {

        // test to confirm RequestURI is encoded: to avoid Sonar java security issue 'S5131' when returning HttpError
        // as a ResponseEntity in the RestExceptionHandler.

        // ARRANGE
        String encoding = StandardCharsets.UTF_8.toString();
        String pathNeedsEncoding = "/this/path changes/when/encoded";
        String pathAfterEncoding = UriUtils.encodePath(pathNeedsEncoding, encoding);

        HttpServletRequest testRequest = mock(HttpServletRequest.class);
        doReturn(pathNeedsEncoding).when(testRequest).getRequestURI();

        // ACT
        final uk.gov.hmcts.ccd.domain.model.common.HttpError error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError(new IllegalArgumentException(MESSAGE), testRequest);

        // ASSERT
        // check returned value different from original
        assertThat(error.getPath(), is(not(equalTo(pathNeedsEncoding))));
        // check returned value matches expected encoded path
        assertThat(error.getPath(), is(equalTo(pathAfterEncoding)));
        // check decoded returned value is same as original
        assertThat(UriUtils.decode(error.getPath(), encoding), is(equalTo(pathNeedsEncoding)));
    }

    @Test
    public void shouldTakeOptionalDetails() {
        final uk.gov.hmcts.ccd.domain.model.common.HttpError<String> error =
                new uk.gov.hmcts.ccd.domain.model.common.HttpError<String>(new IllegalArgumentException(MESSAGE),
                        request)
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
