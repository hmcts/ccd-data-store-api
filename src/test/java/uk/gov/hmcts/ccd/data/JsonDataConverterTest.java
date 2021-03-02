package uk.gov.hmcts.ccd.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class JsonDataConverterTest {
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private JsonDataConverter jsonbConverter;

    @Before
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
        assertEquals("value", converted.get("key").asText());

        try {
            jsonbConverter.convertToEntityAttribute("hjkdash\"");
            fail("Expected failure due to incorrect JSON");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

}
