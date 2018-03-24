package uk.gov.hmcts.ccd.domain.types.sanitiser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Named
@Singleton
public class ComplexSanitiser implements Sanitiser {

    public static final String TYPE = "Complex";
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    private Map<String, Sanitiser> sanitisers;

    @Inject
    public void setSanitisers(List<Sanitiser> sanitisers) {
        this.sanitisers = new HashMap<>();
        sanitisers.forEach(sanitiser -> {
            this.sanitisers.put(sanitiser.getType(), sanitiser);
        });
        // Include itself for nested complex types
        this.sanitisers.put(TYPE, this);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public JsonNode sanitise(FieldType fieldType, JsonNode fieldData) {
        final ObjectNode sanitisedData = JSON_NODE_FACTORY.objectNode();
        final Map<String, CaseField> fieldsMap = new HashMap<>();

        fieldType.getComplexFields().forEach(field -> {
            fieldsMap.put(field.getId(), field);
        });

        final Iterator<Map.Entry<String, JsonNode>> fields = fieldData.fields();

        while(fields.hasNext()) {
            final Map.Entry<String, JsonNode> field = fields.next();

            final String key = field.getKey();
            final JsonNode value = field.getValue();

            if (fieldsMap.containsKey(key)) {
                final CaseField caseField = fieldsMap.get(key);
                final FieldType childType = caseField.getFieldType();

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
