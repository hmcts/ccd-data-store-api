package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
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

        if (!dataValue.isTextual()) {
            return Collections.singletonList(new ValidationResult(dataValue + " is not " + TYPE_ID,
                dataFieldId));
        }

        final List<ValidationResult> results = new ArrayList<>();

        if (!"YES".equalsIgnoreCase(dataValue.textValue()) && !"NO".equalsIgnoreCase(dataValue.textValue())) {
            results.add(new ValidationResult("YES_NO values needs to be YES or NO.  Given value is "
                + dataValue.textValue(), dataFieldId));
        }

        return results;
    }
}
