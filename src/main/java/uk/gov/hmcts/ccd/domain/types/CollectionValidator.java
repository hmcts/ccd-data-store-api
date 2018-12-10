package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.*;

@Named
@Singleton
public class CollectionValidator implements BaseTypeValidator {
    private static final String TYPE_ID = "Collection";
    public static final String VALUE = "value";
    private static final String ID = "id";
    private static final String FIELD_SEPARATOR = ".";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseField caseFieldDefinition) {

        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }

        if (!dataValue.isArray()) {
            final ValidationResult result = new ValidationResult(
                "Require value to be an array",
                dataFieldId);
            return Collections.singletonList(result);
        }

        final ArrayNode arrayValue = (ArrayNode) dataValue;
        final Integer collectionSize = arrayValue.size();
        final FieldType fieldType = caseFieldDefinition.getFieldType();

        final ArrayList<ValidationResult> validationResults = new ArrayList<>();

        if (null != fieldType.getMin()) {
            final Long min = fieldType.getMin().longValue();

            if (min.compareTo(collectionSize.longValue()) > 0) {
                final ValidationResult result = new ValidationResult(
                    String.format("Add at least %d %s", min, min == 1 ? VALUE : "values"),
                    dataFieldId);
                validationResults.add(result);
            }
        }

        if (null != fieldType.getMax()) {
            final Long max = fieldType.getMax().longValue();

            if (max.compareTo(collectionSize.longValue()) < 0) {
                final ValidationResult result = new ValidationResult(
                    String.format("Cannot add more than %d %s", max, max == 1 ? VALUE : "values"),
                    dataFieldId);
                validationResults.add(result);
            }
        }

        final Set<String> ids = new HashSet<>();
        final Iterator<JsonNode> items = dataValue.elements();

        Integer index = 0;
        while (items.hasNext()) {
            final String itemFieldId = dataFieldId + FIELD_SEPARATOR + index;

            final JsonNode item = items.next();

            if (!item.hasNonNull(VALUE)) {
                final ValidationResult result = new ValidationResult("`value` property missing for collection item", itemFieldId);
                validationResults.add(result);
            }

            if (item.hasNonNull(ID)) {
                final JsonNode itemId = item.get(ID);
                if (!itemId.isTextual()) {
                    final ValidationResult result = new ValidationResult("Collection item ID must be a string", itemFieldId);
                    validationResults.add(result);
                }

                if (ids.contains(itemId.textValue())) {
                    final ValidationResult result = new ValidationResult("Collection item ID must be unique", itemFieldId);
                    validationResults.add(result);
                }

                ids.add(itemId.textValue());
            }

            index++;
        }

        return validationResults;
    }
}
