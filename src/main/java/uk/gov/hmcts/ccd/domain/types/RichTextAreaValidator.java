package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Named
@Singleton
public class RichTextAreaValidator implements BaseTypeValidator {
    static final String TYPE_ID = "RichTextArea";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {
        if (Boolean.TRUE.equals(isNullOrEmpty(dataValue))) {
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

        return Collections.emptyList();
    }
}
