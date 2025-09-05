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
    private static final String EMAIL_PATTERN = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

    @Autowired
    @Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
    DefaultCaseDefinitionRepository defaultCaseDefinitionRepository;

    public String redact(final String description) {
        BaseType emailBaseType = getEmailBaseType1();
        if (emailBaseType == null) {
            jclogger.jclog("redact() #1","emailBaseType == null");
        } else if (emailBaseType.getRegularExpression() == null) {
            jclogger.jclog("redact() #1", "emailBaseType.getRegularExpression() == null");
        } else {
            jclogger.jclog("redact() #1", emailBaseType.getRegularExpression());
        }

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
            return description.replaceAll(EMAIL_PATTERN, "[REDACTED EMAIL]");
        }
    }

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

    private BaseType getEmailBaseType2() {
        try {
            jclogger.jclog("getEmailBaseType2()","defaultCaseDefinitionRepository = " +
                defaultCaseDefinitionRepository.toString());
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
