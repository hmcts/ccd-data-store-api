package uk.gov.hmcts.ccd.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InvalidUrlExceptionTest {

    @Test
    public void shouldSetMessageAndCauseWhenCreated() {
        String message = "invalid url";
        Throwable cause = new IllegalArgumentException("bad url");

        InvalidUrlException exception = new InvalidUrlException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
