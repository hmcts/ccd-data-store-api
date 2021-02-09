package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;

@Named
@Singleton
public class CollectionValidator implements BaseTypeValidator {
    public static final String VALUE = "value";
    public static final String ID = "id";
    private static final String FIELD_SEPARATOR = ".";

    @Override
    public BaseType getType() {
        return BaseType.get(COLLECTION);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {

        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }

        if (!dataValue.isArray()) {
            return Collections.singletonList(new ValidationResult("Require value to be an array",
                dataFieldId));
        }

        final ArrayNode arrayValue = (ArrayNode) dataValue;
        final Integer collectionSize = arrayValue.size();
        final FieldTypeDefinition fieldTypeDefinition = caseFieldDefinition.getFieldTypeDefinition();

        final ArrayList<ValidationResult> validationResults = new ArrayList<>();

        if (null != fieldTypeDefinition.getMin()) {
            final Long min = fieldTypeDefinition.getMin().longValue();

            if (min.compareTo(collectionSize.longValue()) > 0) {
                final ValidationResult result = new ValidationResult(
                    String.format("Add at least %d %s", min, min == 1 ? VALUE : "values"),
                    dataFieldId);
                validationResults.add(result);
            }
        }

        if (null != fieldTypeDefinition.getMax()) {
            final Long max = fieldTypeDefinition.getMax().longValue();

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
                final ValidationResult result =
                    new ValidationResult("`value` property missing for collection item", itemFieldId);
                validationResults.add(result);
            }

            if (item.hasNonNull(ID)) {
                final JsonNode itemId = item.get(ID);
                if (!itemId.isTextual()) {
                    final ValidationResult result =
                        new ValidationResult("Collection item ID must be a string", itemFieldId);
                    validationResults.add(result);
                }

                if (ids.contains(itemId.textValue())) {
                    final ValidationResult result =
                        new ValidationResult("Collection item ID must be unique", itemFieldId);
                    validationResults.add(result);
                }

                ids.add(itemId.textValue());
            }

            index++;
        }

        return validationResults;
    }
}
