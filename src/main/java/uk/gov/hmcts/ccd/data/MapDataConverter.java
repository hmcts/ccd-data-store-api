package uk.gov.hmcts.ccd.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class MapDataConverter implements AttributeConverter<Map<String, Object>, String> {
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String convertToDatabaseColumn(final Map<String, Object> objectValue) {
        String dataValue;
        try {
            dataValue = mapper.writeValueAsString(objectValue);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Unable to serialize json field", e);
        }
        return dataValue;
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(final String dataValue) {
        try {
            if (dataValue == null) {
                return null;
            }
            return mapper.readValue(dataValue, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to deserialize to json field", e);
        }
    }
}
