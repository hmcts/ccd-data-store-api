package uk.gov.hmcts.ccd.domain.types.sanitiser;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

@Named
@Singleton
public class CollectionSanitiser implements Sanitiser {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    public static final String VALUE = "value";
    public static final String ID = "id";

    private Map<String, Sanitiser> sanitisers;

    @Inject
    public void setSanitisers(List<Sanitiser> sanitisers) {
        this.sanitisers = new HashMap<>();
        sanitisers.forEach(sanitiser -> {
            this.sanitisers.put(sanitiser.getType(), sanitiser);
        });
    }

    @Override
    public String getType() {
        return COLLECTION;
    }

    @Override
    public JsonNode sanitise(FieldTypeDefinition fieldTypeDefinition, JsonNode fieldData) {
        final ArrayNode sanitisedData = JSON_NODE_FACTORY.arrayNode();

        final FieldTypeDefinition itemType = fieldTypeDefinition.getCollectionFieldTypeDefinition();

        fieldData.forEach(item -> {
            if (item.hasNonNull(VALUE)) {
                final ObjectNode sanitisedItem = JSON_NODE_FACTORY.objectNode();
                final JsonNode itemValue = item.get(VALUE);

                // Sanitise value
                if (sanitisers.containsKey(itemType.getType())) {
                    final Sanitiser sanitiser = sanitisers.get(itemType.getType());
                    sanitisedItem.set(VALUE, sanitiser.sanitise(itemType, itemValue));
                } else {
                    sanitisedItem.set(VALUE, itemValue);
                }

                // Generate missing IDs
                if (item.hasNonNull(ID) && item.get(ID).isTextual()) {
                    sanitisedItem.set(ID, item.get(ID));
                } else {
                    sanitisedItem.set(ID, JSON_NODE_FACTORY.textNode(UUID.randomUUID().toString()));
                }

                sanitisedData.add(sanitisedItem);
            }
        });

        return sanitisedData;
    }
}
