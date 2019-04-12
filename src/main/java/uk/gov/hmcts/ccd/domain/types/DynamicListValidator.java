package uk.gov.hmcts.ccd.domain.types;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

@Named
@Singleton
public class DynamicListValidator implements BaseTypeValidator {
    protected static final String TYPE_ID = "DynamicList";
    private static final String DYNAMIC_LIST_ITEMS = "dynamic_list_items";
    private static final String CODE = "code";
    private static final String DEFAULT = "default";
    private static final String LABEL = "label";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(String dataFieldId, JsonNode dataValue, CaseField caseFieldDefinition) {
        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }
        List<ValidationResult> results = new ArrayList<>();

        dataValue.get(DYNAMIC_LIST_ITEMS).elements().forEachRemaining(node -> validateLength(results, node, dataFieldId));
        validateLength(results, dataValue.get(DEFAULT), dataFieldId);

        return results;
    }

    private void validateLength(List<ValidationResult> results, JsonNode node, String dataFieldId) {
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
