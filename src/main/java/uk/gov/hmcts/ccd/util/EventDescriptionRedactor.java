package uk.gov.hmcts.ccd.util;

public class EventDescriptionRedactor {
    // TODO: Resolve compliance with S5852 (polynomial runtime regex).
    //       See EmailValidator , EmailValidatorTest , BaseType.
    private static final String EMAIL_PATTERN = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

    public String redact(final String description) {
        if (description == null) {
            return null;
        } else {
            return description.replaceAll(EMAIL_PATTERN, "[REDACTED EMAIL]");
        }
    }

}
