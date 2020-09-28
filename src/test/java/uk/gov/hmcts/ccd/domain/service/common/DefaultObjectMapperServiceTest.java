package uk.gov.hmcts.ccd.domain.service.common;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

class DefaultObjectMapperServiceTest {

    private DefaultObjectMapperService objectMapperService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        objectMapperService = new DefaultObjectMapperService(objectMapper);
    }

    @Nested
    @DisplayName("convertStringToObject()")
    class ConvertStringToObject {

        @Test
        @DisplayName("should convert a string to object")
        void shouldConvertStringToObject() {
            String json = "{\"testString\":\"test\",\"testInteger\":1}";
            JsonTestDto jsonTest = objectMapperService.convertStringToObject(json, JsonTestDto.class);

            assertThat(jsonTest.getTestString(), is("test"));
            assertThat(jsonTest.getTestInteger(), is(1));
        }

        @Test
        @DisplayName("should throw exception for malformed json string conversion to object")
        void shouldThrowExceptionOnConvertStringToObject() {
            String json = "{\"testString\":\"test\" \"testInteger\":1}";
            assertThrows(ServiceException.class, () ->
                    objectMapperService.convertStringToObject(json, JsonTestDto.class));
        }

    }

    @Nested
    @DisplayName("convertObjectToString()")
    class ConvertObjectToString {

        @Test
        @DisplayName("should convert object to string")
        void shouldConvertObjectToString() {
            JsonTestDto jsonTest = new JsonTestDto();
            jsonTest.setTestInteger(1);
            jsonTest.setTestString("test");

            String expectedJson = "{\"testString\":\"test\",\"testInteger\":1}";
            String jsonString = objectMapperService.convertObjectToString(jsonTest);

            assertThat(jsonString, is(expectedJson));
        }

        @Test
        @DisplayName("should throw exception for errors on converting object to json string")
        void shouldThrowExceptionOnConvertObjectToString() throws JsonProcessingException {
            ObjectMapper objectMapper = mock(ObjectMapper.class);
            DefaultObjectMapperService service = new DefaultObjectMapperService(objectMapper);
            doThrow(new JsonParseException(null, "")).when(objectMapper).writeValueAsString(any());

            assertThrows(ServiceException.class, () -> service.convertObjectToString(new JsonTestDto()));
        }

    }

    @Nested
    @DisplayName("convertObjectToJsonNode()")
    class ConvertObjectToJsonNode {

        @Test
        @DisplayName("should convert object to json node")
        void shouldConvertObjectToJsonNode() {
            Map<String, JsonNode> data = new HashMap<>();
            ObjectNode node = objectMapper.createObjectNode();
            node.put("field", "value");
            data.put("data", node);

            String expectedJson = "{\"data\":{\"field\":\"value\"}}";
            JsonNode jsonNode = objectMapperService.convertObjectToJsonNode(data);

            assertThat(jsonNode.toString(), is(expectedJson));
        }

    }

    @Nested
    @DisplayName("convertJsonNodeToMap()")
    class ConvertJsonNodeToMap {

        @Test
        @DisplayName("should convert json node to map")
        void shouldConvertObjectToString() {
            ObjectNode childNode = objectMapper.createObjectNode();
            childNode.put("field", "value");
            ObjectNode parentNode = objectMapper.createObjectNode();
            parentNode.set("data", childNode);

            Map<String, JsonNode> map = objectMapperService.convertJsonNodeToMap(parentNode);

            assertThat(map.containsKey("data"), is(true));
            assertThat(map.get("data").toString(), is("{\"field\":\"value\"}"));
        }

        @Test
        @DisplayName("should throw exception for errors on converting object to json string")
        void shouldThrowExceptionOnConvertObjectToString() throws JsonProcessingException {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("field", "value");
            ObjectMapper objectMapper = mock(ObjectMapper.class);
            DefaultObjectMapperService service = new DefaultObjectMapperService(objectMapper);
            doThrow(new IllegalArgumentException("")).when(objectMapper).convertValue(any(),
                    Matchers.<TypeReference<HashMap<String, JsonNode>>>any());

            assertThrows(ServiceException.class, () -> service.convertJsonNodeToMap(node));
        }

    }


    @Nested
    @DisplayName("createEmptyJsonNode()")
    class CreateEmptyJsonNode {

        @Test
        @DisplayName("should create empty json node")
        void shouldCreateEmptyJsonNode() {
            assertThat(objectMapperService.createEmptyJsonNode(), is(instanceOf(JsonNode.class)));
        }

    }

}
