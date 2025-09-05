package uk.gov.hmcts.ccd.domain.types;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.service.common.JcLogger;

@Component
public class EventDescriptionRedactor {
    private final JcLogger jclogger = new JcLogger("EventDescriptionRedactor", true);

    // TODO: Resolve compliance with S5852 (polynomial runtime regex).
    //       See EmailValidator , EmailValidatorTest , BaseType.

    // Original EMAIL_PATTERN
    private static final String EMAIL_PATTERN1 = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
    // Improved, S5852-compliant version
    private static final String EMAIL_PATTERN2 = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,10}";
    // Optional (with atomic groups for extra safety, if Java 9+)
    private static final String EMAIL_PATTERN3 = "(?>[a-zA-Z0-9._%+-]+)@(?>[a-zA-Z0-9.-]+)\\.[a-zA-Z]{2,10}";

    @Autowired
    @Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
    DefaultCaseDefinitionRepository defaultCaseDefinitionRepository;

    public String redact(final String description) {
        // The result of below is "emailBaseType.getRegularExpression() == null"
        BaseType emailBaseType = getEmailBaseType1();
        if (emailBaseType == null) {
            jclogger.jclog("redact() #1","emailBaseType == null");
        } else if (emailBaseType.getRegularExpression() == null) {
            jclogger.jclog("redact() #1", "emailBaseType.getRegularExpression() == null");
        } else {
            jclogger.jclog("redact() #1", emailBaseType.getRegularExpression());
        }

        // The result of below is "emailBaseType == null"
        emailBaseType = getEmailBaseType2();
        if (emailBaseType == null) {
            jclogger.jclog("redact() #2","emailBaseType == null");
        } else if (emailBaseType.getRegularExpression() == null) {
            jclogger.jclog("redact() #2", "emailBaseType.getRegularExpression() == null");
        } else {
            jclogger.jclog("redact() #2", emailBaseType.getRegularExpression());
        }

        if (description == null) {
            return null;
        } else {
            return description.replaceAll(EMAIL_PATTERN3, "[REDACTED EMAIL]");
        }
    }

    // Does return an emailBaseType , but
    private BaseType getEmailBaseType1() {
        try {
            final BaseType emailBaseType = BaseType.get("Email");
            jclogger.jclog("getEmailBaseType1()","emailBaseType = " + emailBaseType.toString());
            return emailBaseType;
        } catch (Exception e) {
            jclogger.jclog("getEmailBaseType1()","emailBaseType == null");
            return null;
        }
    }

    // Suspect Autowired defaultCaseDefinitionRepository == null , but to be confirmed.
    private BaseType getEmailBaseType2() {
        try {
            jclogger.jclog("getEmailBaseType2()","defaultCaseDefinitionRepository = "
                + (defaultCaseDefinitionRepository == null ? "" : defaultCaseDefinitionRepository.toString()));
            BaseType.setCaseDefinitionRepository(defaultCaseDefinitionRepository);
            final BaseType emailBaseType = BaseType.get("Email");
            jclogger.jclog("getEmailBaseType2()","emailBaseType = " + emailBaseType.toString());
            return emailBaseType;
        } catch (Exception e) {
            jclogger.jclog("getEmailBaseType2()","emailBaseType == null");
            return null;
        }
    }
}
