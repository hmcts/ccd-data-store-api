package uk.gov.hmcts.ccd.domain.types.sanitiser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.TEXT;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class TextSanitiser implements Sanitiser {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    @Override
    public String getType() {
        return TEXT;
    }

    public JsonNode sanitise(FieldType fieldType, JsonNode fieldData) {
        if (fieldData.isTextual()) {
            return JSON_NODE_FACTORY.textNode(fieldData.asText().trim());
        }
        return fieldData;
    }
}
