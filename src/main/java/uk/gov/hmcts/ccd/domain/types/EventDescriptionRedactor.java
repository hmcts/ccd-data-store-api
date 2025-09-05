package uk.gov.hmcts.ccd.domain.types;

public class EventDescriptionRedactor {
    private static final String EMAIL_PATTERN = "(?>[a-zA-Z0-9._%+-]+)@(?>[a-zA-Z0-9.-]+)\\.[a-zA-Z]{2,10}";

    public String redact(final String description) {
        if (description == null) {
            return null;
        } else {
            return description.replaceAll(EMAIL_PATTERN, "[REDACTED EMAIL]");
        }
    }
}
