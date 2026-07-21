package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import java.util.Collections;
import java.util.List;

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
        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }

        if (!dataValue.isTextual()) {
            final String nodeType = dataValue.getNodeType().toString().toLowerCase();
            return Collections.singletonList(new ValidationResult(nodeType + " is not a string", dataFieldId));
        }

        final String value = dataValue.textValue();

        if (!TextValidator.checkMin(caseFieldDefinition.getFieldTypeDefinition().getMin(), value)) {
            return Collections.singletonList(
                new ValidationResult("requires a minimum length of "
                    + caseFieldDefinition.getFieldTypeDefinition().getMin(), dataFieldId)
            );
        }

        return Collections.emptyList();
    }
}
