package uk.gov.hmcts.ccd.util;

public class EventDescriptionRedactor {
    private static final String EMAIL_PATTERN = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

    public String redact(final String description) {
        return description.replaceAll(EMAIL_PATTERN, "[REDACTED EMAIL]");
    }

}
