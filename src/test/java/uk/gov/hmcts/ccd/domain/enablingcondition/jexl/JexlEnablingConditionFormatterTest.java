package uk.gov.hmcts.ccd.domain.enablingcondition.jexl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JexlEnablingConditionFormatterTest {

    private JexlEnablingConditionFormatter enablingConditionFormatter;

    @BeforeEach
    void setUp() {
        enablingConditionFormatter = new JexlEnablingConditionFormatter();
    }

    @Test
    void formatEnablingConditionWithAndOperator() {
        String formatString = this.enablingConditionFormatter
            .format("FieldA!=\"\" AND FieldB=\"I'm innocent\"");

        assertNotNull(formatString);
        assertEquals("FieldA!=\"\" and FieldB==\"I'm innocent\"", formatString);
    }

    @Test
    void formatEnablingConditionWithOrOperator() {
        String formatString = this.enablingConditionFormatter
            .format("FieldA!=\"\" OR FieldB!=\"I'm innocent\"");

        assertNotNull(formatString);
        assertEquals("FieldA!=\"\" or FieldB!=\"I'm innocent\"", formatString);
    }

    @Test
    void formatEmptyEnablingCondition() {
        String formatString = this.enablingConditionFormatter.format("");

        assertNotNull(formatString);
        assertEquals("", formatString);
    }

    @Test
    void formatNullEnablingCondition() {
        String formatString = this.enablingConditionFormatter.format(null);
        assertNull(formatString);
    }

    @Test
    void formatEnablingConditionWithRegularExpression() {
        String formatString = this.enablingConditionFormatter
            .format("FieldA!=\"*\" AND FieldB=\"I'm innocent\"");

        assertNotNull(formatString);
        assertEquals("FieldA!~\".*\" and FieldB==\"I'm innocent\"", formatString);
    }

    @Test
    void formatEnablingConditionWithEqualityRegularExpression() {
        String formatString = this.enablingConditionFormatter
            .format("FieldA=\"*\" AND FieldB=\"I'm innocent\"");

        assertNotNull(formatString);
        assertEquals("FieldA=~\".*\" and FieldB==\"I'm innocent\"", formatString);
    }

}
