package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CasePersistenceExceptionTest {

    @Test
    public void shouldSetMessageWhenCreated() {
        String message = "case persistence failed";

        CasePersistenceException exception = new CasePersistenceException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(ApiException.class);
    }
}
