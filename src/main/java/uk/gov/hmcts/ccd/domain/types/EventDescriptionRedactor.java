package uk.gov.hmcts.ccd.domain.types;

import uk.gov.hmcts.ccd.domain.service.common.JcLogger;

public class EventDescriptionRedactor {
    private final JcLogger jclogger = new JcLogger("EventDescriptionRedactor", true);

    // TODO: Resolve compliance with S5852 (polynomial runtime regex).
    //       See EmailValidator , EmailValidatorTest , BaseType.
    private static final String EMAIL_PATTERN = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

    private final BaseType emailBaseType = BaseType.get("Email");

    public String redact(final String description) {
        if (emailBaseType == null) {
            jclogger.jclog("redact() emailBaseType == null");
        } else {
            jclogger.jclog("redact() " + emailBaseType.getRegularExpression());
        }

        if (description == null) {
            return null;
        } else {
            return description.replaceAll(EMAIL_PATTERN, "[REDACTED EMAIL]");
        }
    }

}
