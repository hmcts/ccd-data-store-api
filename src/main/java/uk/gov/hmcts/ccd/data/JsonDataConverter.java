package uk.gov.hmcts.ccd.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class JsonDataConverter implements AttributeConverter<JsonNode, String> {
    private static final ObjectMapper MAPPER = JsonMapper.builderWithJackson2Defaults()
        .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
        .build();

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
            return MAPPER.readTree(dataValue);
        } catch (JacksonException e) {
            throw new RuntimeException("Unable to deserialize to json field", e);
        }
    }
}
