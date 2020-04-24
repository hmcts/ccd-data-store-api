package uk.gov.hmcts.ccd.domain.types.sanitiser;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

@Named
@Singleton
public class ComplexSanitiser implements Sanitiser {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    private Map<String, Sanitiser> sanitisers;

    @Inject
    public void setSanitisers(List<Sanitiser> sanitisers) {
        this.sanitisers = new HashMap<>();
        sanitisers.forEach(sanitiser -> {
            this.sanitisers.put(sanitiser.getType(), sanitiser);
        });
        // Include itself for nested complex types
        this.sanitisers.put(COMPLEX, this);
    }

    @Override
    public String getType() {
        return COMPLEX;
    }

    @Override
    public JsonNode sanitise(FieldTypeDefinition fieldTypeDefinition, JsonNode fieldData) {
        final ObjectNode sanitisedData = JSON_NODE_FACTORY.objectNode();
        final Map<String, CaseFieldDefinition> fieldsMap = new HashMap<>();

        fieldTypeDefinition.getComplexFields().forEach(field -> {
            fieldsMap.put(field.getId(), field);
        });

        final Iterator<Map.Entry<String, JsonNode>> fields = fieldData.fields();

        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> field = fields.next();

            final String key = field.getKey();
            final JsonNode value = field.getValue();

            if (fieldsMap.containsKey(key)) {
                final CaseFieldDefinition caseFieldDefinition = fieldsMap.get(key);
                final FieldTypeDefinition childType = caseFieldDefinition.getFieldTypeDefinition();

                if (sanitisers.containsKey(childType.getType())) {
                    final Sanitiser sanitiser = sanitisers.get(childType.getType());
                    sanitisedData.set(key, sanitiser.sanitise(childType, value));
                } else {
                    sanitisedData.set(key, value);
                }
            }
        }

        return sanitisedData;
    }
}
