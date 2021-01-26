package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

@Named
@Singleton
public class DynamicListValidator implements BaseTypeValidator {
    protected static final String TYPE_ID = "DynamicList";
    private static final String LIST_ITEMS = "list_items";
    private static final String CODE = "code";
    protected static final String VALUE = "value";
    private static final String LABEL = "label";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(String dataFieldId, JsonNode dataValue,
                                           CaseFieldDefinition caseFieldDefinition) {
        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }
        List<ValidationResult> results = new ArrayList<>();

        dataValue.get(LIST_ITEMS).elements().forEachRemaining(node -> validateLength(results, node, dataFieldId));
        JsonNode value = dataValue.get(VALUE);
        validateValueField(value, dataFieldId, results);
        return results;
    }

    protected void validateValueField(JsonNode value,
                                      String dataFieldId,
                                      List<ValidationResult> results) {
        if (value != null) {
            if (value.isArray()) {
                results.add(new ValidationResult(
                    String.format("Array values are not supported for '%s' type", getType()), dataFieldId));
                return;
            }
            validateLength(results, value, dataFieldId);
        }
    }

    protected void validateLength(List<ValidationResult> results, JsonNode node, String dataFieldId) {
        final String code = node.get(CODE).textValue();
        final String value = node.get(LABEL).textValue();
        if (StringUtils.isNotEmpty(code) && code.length() > 150) {
            results.add(new ValidationResult("Code length exceeds MAX limit", dataFieldId));
        }
        if (StringUtils.isNotEmpty(value) && value.length() > 250) {
            results.add(new ValidationResult("Value length exceeds MAX limit", dataFieldId));
        }

    }
}
