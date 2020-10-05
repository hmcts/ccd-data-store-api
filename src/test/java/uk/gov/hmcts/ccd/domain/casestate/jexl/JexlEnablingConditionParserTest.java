package uk.gov.hmcts.ccd.domain.casestate.jexl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JexlEnablingConditionParserTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    private JexlEnablingConditionParser enablingConditionParser;

    @BeforeEach
    void setUp() {
        this.enablingConditionParser = new JexlEnablingConditionParser(new JexlEnablingConditionFormatter());
    }

    @Test
    void evaluateEnablingConditionWhenValidDataPresent() {
        String enablingCondition = "FieldA!=\"\" AND FieldB=\"I'm innocent\")";
        Boolean isValid = this.enablingConditionParser.evaluate(enablingCondition, createCaseData(
            "Test",
            "I'm innocent"
        ));

        assertNotNull(isValid);
        assertEquals(true, isValid);
    }

    @Test
    void evaluateEnablingConditionWhenValidDataNotPresent() {
        String enablingCondition = "FieldA!=\"\" AND FieldB=\"I'm innocent\")";
        Boolean isValid = this.enablingConditionParser.evaluate(enablingCondition, createCaseData(
            "",
            "I'm innocent"
        ));

        assertNotNull(isValid);
        assertEquals(false, isValid);
    }

    @Test
    void evaluateEnablingConditionWhenConditionContainsRegularExpression() {
        String enablingCondition = "FieldA=\"*\" AND FieldB=\"I'm innocent\")";
        Boolean isValid = this.enablingConditionParser.evaluate(enablingCondition, createCaseData(
            "Test",
            "I'm innocent"
        ));

        assertNotNull(isValid);
        assertEquals(true, isValid);
    }

    @Test
    void evaluateEnablingConditionWhenConditionContainsNotEqualityRegularExpression() {
        String enablingCondition = "FieldA!=\"*\" AND FieldB=\"I'm innocent\")";
        Boolean isValid = this.enablingConditionParser.evaluate(enablingCondition, createCaseData(
            "Test",
            "I'm innocent"
        ));

        assertNotNull(isValid);
        assertEquals(false, isValid);
    }

    @Test
    void evaluateEnablingConditionWhenConditionIsNull() {
        Boolean isValid = this.enablingConditionParser.evaluate(null, createCaseData(
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
