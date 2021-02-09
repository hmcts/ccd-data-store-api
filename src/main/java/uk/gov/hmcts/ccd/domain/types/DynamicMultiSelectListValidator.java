package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class DynamicMultiSelectListValidator extends DynamicListValidator {
    protected static final String TYPE_ID = "DynamicMultiSelectList";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    protected void validateValueField(JsonNode value,
                                      String dataFieldId,
                                      List<ValidationResult> results) {
        if (value != null && value.isArray()) {
            value.elements()
                .forEachRemaining(node -> validateLength(results, node, dataFieldId));
        }
    }

}
