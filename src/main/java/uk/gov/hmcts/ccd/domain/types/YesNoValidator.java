package uk.gov.hmcts.ccd.domain.types;

import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Named
@Singleton
public class YesNoValidator implements BaseTypeValidator {
    public static final String TYPE_ID = "YesOrNo";

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

        if (!dataValue.isString()) {
            return Collections.singletonList(new ValidationResult(dataValue + " is not " + TYPE_ID,
                dataFieldId));
        }

        final List<ValidationResult> results = new ArrayList<>();

        if (!"YES".equalsIgnoreCase(dataValue.stringValue(null))
            && !"NO".equalsIgnoreCase(dataValue.stringValue(null))) {
            results.add(new ValidationResult("YES_NO values needs to be YES or NO.  Given value is "
                + dataValue.stringValue(null), dataFieldId));
        }

        return results;
    }
}
