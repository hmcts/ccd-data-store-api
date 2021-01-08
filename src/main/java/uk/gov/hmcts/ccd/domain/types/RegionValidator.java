package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Named("RegionValidator")
@Singleton
public class RegionValidator implements BaseTypeValidator {
    public static final String TYPE_ID = "Region";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {

        // Empty text should still check against MIN - MIN may or may not be 0
        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }

        if (!dataValue.isTextual()) {
            final String nodeType = dataValue.getNodeType().toString().toLowerCase();
            return Collections.singletonList(new ValidationResult(nodeType + " is not a string",
                dataFieldId)
            );
        }

        final String value = dataValue.textValue();

        List<ValidationResult> response = (validateRegex(caseFieldDefinition, value, dataFieldId));
        if ((response != null)) {
            return response;
        }

        response = (validateMin(caseFieldDefinition, value, dataFieldId));
        if ((response != null)) {
            return response;
        }

        response = (validateMax(caseFieldDefinition, value, dataFieldId));
        if ((response != null)) {
            return response;
        }

        return Collections.emptyList();

    }
}
