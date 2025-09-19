package uk.gov.hmcts.ccd.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventDescriptionRedactorTest {

    private final EventDescriptionRedactor redactor = new EventDescriptionRedactor();

    @Test
    void shouldReturnNullIfInputIsNull() {
        assertNull(redactor.redact(null));
    }

    @ParameterizedTest
    @CsvSource({
        "Plain text, Plain text",
        "Contact me at john.doe@example.com for details, Contact me at [REDACTED EMAIL] for details",
        "Emails: alice@example.com; bob.smith@domain.co.uk, Emails: [REDACTED EMAIL]; [REDACTED EMAIL]"
    })
    void testRedactor(String input, String expected) {
        assertEquals(expected, redactor.redact(input));
    }
}
