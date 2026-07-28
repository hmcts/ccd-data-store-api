package uk.gov.hmcts.ccd.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class JsonDataConverterTest {
    private static final ObjectMapper mapper = JsonMapper.builderWithJackson2Defaults()
        .changeDefaultPropertyInclusion(inclusion ->
            inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
        .build();

    private JsonDataConverter jsonbConverter;

    @BeforeEach
    public void setup() {
        jsonbConverter = new JsonDataConverter();
    }

    @Test
    public void convertToDatabaseColumn() throws Exception {
        assertNull(jsonbConverter.convertToDatabaseColumn(null));

        final String jsonString = "{\"key\":\"value\"}";
        assertEquals(jsonString, jsonbConverter.convertToDatabaseColumn(mapper.readTree(jsonString)));
    }

    @Test
    public void convertToEntityAttribute() {
        // Testing null
        assertNull(jsonbConverter.convertToEntityAttribute(null));

        // Teasing valid non null
        final JsonNode converted = jsonbConverter.convertToEntityAttribute("{\"key\":\"value\"}");
        assertEquals("value", converted.get("key").asString());

        try {
            jsonbConverter.convertToEntityAttribute("hjkdash\"");
            fail("Expected failure due to incorrect JSON");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

}
