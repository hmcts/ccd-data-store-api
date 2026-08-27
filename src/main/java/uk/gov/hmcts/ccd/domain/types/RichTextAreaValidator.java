package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.types.listener.RichTextAreaValidationListener;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Named
@Singleton
public class RichTextAreaValidator implements BaseTypeValidator {
    static final String TYPE_ID = "RichTextArea";

    @Value("${html.policy.allowed.whitelist.tags:b}")
    private String[] allowedWhitelistTags;

    private static final Pattern HTML_TAG_PAIR =
        Pattern.compile("<([A-Za-z][A-Za-z0-9]*)\\b[^>]*>[\\s\\S]*?</\\1>");


    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {
        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }

        if (!dataValue.isTextual()) {
            final String nodeType = dataValue.getNodeType().toString().toLowerCase(Locale.ROOT);
            return Collections.singletonList(new ValidationResult(nodeType + " is not a string", dataFieldId));
        }

        final String value = dataValue.textValue();

        final BigDecimal minLength = caseFieldDefinition.getFieldTypeDefinition().getMin();

        if (!TextValidator.checkMin(minLength, value)) {
            return Collections.singletonList(
                new ValidationResult("requires a minimum length of " + minLength, dataFieldId)
            );
        }

        validateHtml(value);

        return Collections.emptyList();
    }

    private void validateHtml(String value) {
        final PolicyFactory policyDefinition = new HtmlPolicyBuilder()
            .allowElements(allowedWhitelistTags).toFactory();

        RichTextAreaValidationListener listener = new RichTextAreaValidationListener();
        policyDefinition.sanitize(value, listener, null);

        if (!containsTagPair(value) || !listener.getErrors().isEmpty()) {
            throw new BadRequestException(
                "Enter valid tags for RichTextArea field"
            );
        }
    }

    private boolean containsTagPair(String input) {
        return input != null && HTML_TAG_PAIR.matcher(input).find();
    }
}
