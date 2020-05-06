package uk.gov.hmcts.ccd.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter(autoApply = true)
public class JsonDataConverter implements AttributeConverter<JsonNode, String> {
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String convertToDatabaseColumn(final JsonNode objectValue) {
        if (objectValue == null) {
            return null;
        }
        return objectValue.toString();
    }

    @Override
    public JsonNode convertToEntityAttribute(final String dataValue) {
        try {
            if (dataValue == null) {
                return null;
            }
            return mapper.readTree(dataValue);
        } catch (IOException e) {
            throw new RuntimeException("Unable to deserialize to json field", e);
        }
    }
}
