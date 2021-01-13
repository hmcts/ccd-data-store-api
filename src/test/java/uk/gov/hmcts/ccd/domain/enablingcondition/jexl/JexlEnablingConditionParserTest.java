package uk.gov.hmcts.ccd.domain.enablingcondition.jexl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

class JexlEnablingConditionParserTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    private JexlEnablingConditionParser enablingConditionParser;

    @BeforeEach
    void setUp() {
        this.enablingConditionParser = new JexlEnablingConditionParser(new JexlEnablingConditionConverter());
    }

    @Test
    void evaluateEnablingConditionWhenValidDataPresent() {
        String enablingCondition = "FieldA!=\"\" AND FieldB=\"I'm innocent\")";
        Boolean isValid = enablingConditionParser.evaluate(enablingCondition, createCaseData(
            "Test",
            "I'm innocent"
        ));

        assertNotNull(isValid);
        assertEquals(true, isValid);
    }

    @Test
    void evaluateEnablingConditionWhenValidDataNotPresent() {
        String enablingCondition = "FieldA!=\"\" AND FieldB=\"I'm innocent\")";
        Boolean isValid = enablingConditionParser.evaluate(enablingCondition, createCaseData(
            "",
            "I'm innocent"
        ));

        assertNotNull(isValid);
        assertEquals(false, isValid);
    }

    @Test
    void evaluateEnablingConditionWhenConditionContainsRegularExpression() {
        String enablingCondition = "FieldA=\"*\" AND FieldB=\"I'm innocent\")";
        Boolean isValid = enablingConditionParser.evaluate(enablingCondition, createCaseData(
            "Test",
            "I'm innocent"
        ));

        assertNotNull(isValid);
        assertEquals(true, isValid);
    }

    @Test
    void evaluateEnablingConditionWhenConditionContainsNotEqualityRegularExpression() {
        String enablingCondition = "FieldA!=\"*\" AND FieldB=\"I'm innocent\")";
        Boolean isValid = enablingConditionParser.evaluate(enablingCondition, createCaseData(
            "Test",
            "I'm innocent"
        ));

        assertNotNull(isValid);
        assertEquals(false, isValid);
    }

    @Test
    void evaluateEnablingConditionWhenConditionIsNull() {
        Boolean isValid = enablingConditionParser.evaluate(null, createCaseData(
            "Test",
            "I'm innocent"
        ));

        assertNotNull(isValid);
        assertEquals(false, isValid);
    }

    @Test
    void shouldReturnFalseWhenConditionHasUnKnownField() {
        String enablingCondition = "FieldC=\"\" AND FieldB=\"I'm innocent\")";
        Boolean isValid = enablingConditionParser.evaluate(enablingCondition, createCaseData(
            "Test",
            "I'm innocent"
        ));

        assertNotNull(isValid);
        assertEquals(false, isValid);
    }

    @Test
    void shouldThrowExceptionWhenDataIsNotValidJson() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        enablingConditionParser = new JexlEnablingConditionParser(new JexlEnablingConditionConverter(),
            objectMapper);
        String enablingCondition = "FieldA!=\"\" AND FieldB=\"I'm innocent\")";
        Boolean isValid = enablingConditionParser.evaluate(enablingCondition, createCaseData(
            "Test",
            "I'm innocent"
        ));

        assertNotNull(isValid);
        assertEquals(false, isValid);
    }

    private Map<String, JsonNode> createCaseData(String fieldA,
                                                 String fieldB) {
        Map<String, JsonNode> data = new HashMap<>();
        data.put("FieldA", JSON_NODE_FACTORY.textNode(fieldA));
        data.put("FieldB", JSON_NODE_FACTORY.textNode(fieldB));
        return data;
    }
}
