package uk.gov.hmcts.ccd.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonDataConverterTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private JsonDataConverter jsonbConverter;

    @BeforeEach
    public void setup() {
        jsonbConverter = new JsonDataConverter();
    }

    @Test
    public void convertToDatabaseColumn_shouldHandleNullAndValidJson() throws JsonProcessingException {
        // Null input
        assertThat(jsonbConverter.convertToDatabaseColumn(null)).isNull();

        // Valid JSON input
        final String jsonString = "{\"key\":\"value\"}";
        JsonNode jsonNode = mapper.readTree(jsonString);
        String result = jsonbConverter.convertToDatabaseColumn(jsonNode);

        assertThat(result).isEqualTo(jsonString);
    }

    @Test
    public void convertToEntityAttribute_shouldHandleNullAndValidJson() {
        // Null input
        assertThat(jsonbConverter.convertToEntityAttribute(null)).isNull();

        // Valid JSON string
        final JsonNode converted = jsonbConverter.convertToEntityAttribute("{\"key\":\"value\"}");
        assertThat(converted.get("key").asText()).isEqualTo("value");
    }

    @Test
    public void convertToEntityAttribute_shouldThrowOnInvalidJson() {
        assertThatThrownBy(() -> jsonbConverter.convertToEntityAttribute("hjkdash\""))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Unable to deserialize to json field");
    }
}
