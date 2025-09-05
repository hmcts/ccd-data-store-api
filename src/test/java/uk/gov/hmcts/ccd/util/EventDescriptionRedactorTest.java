package uk.gov.hmcts.ccd.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventDescriptionRedactorTest {

    private final EventDescriptionRedactor redactor = new EventDescriptionRedactor();

    @Test
    void shouldReturnNullIfInputIsNull() {
        assertNull(redactor.redact(null));
    }

    @Test
    void shouldNotRedactPlainText() {
        String input = "Plain text.";
        String expected = "Plain text.";
        assertEquals(expected, redactor.redact(input));
    }

    @Test
    void shouldRedactSingleEmail() {
        String input = "Contact me at john.doe@example.com for details.";
        String expected = "Contact me at [REDACTED EMAIL] for details.";
        assertEquals(expected, redactor.redact(input));
    }

    @Test
    void shouldRedactMultipleEmails() {
        String input = "Emails: alice@example.com, bob.smith@domain.co.uk";
        String expected = "Emails: [REDACTED EMAIL], [REDACTED EMAIL]";
        assertEquals(expected, redactor.redact(input));
    }
}
