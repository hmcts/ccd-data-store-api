package uk.gov.hmcts.ccd.domain.enablingcondition.jexl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JexlEnablingConditionConverterTest {

    private JexlEnablingConditionConverter enablingConditionFormatter;

    @BeforeEach
    void setUp() {
        enablingConditionFormatter = new JexlEnablingConditionConverter();
    }

    @Test
    void formatEnablingConditionWithAndOperator() {
        String formatString = this.enablingConditionFormatter
            .convert("FieldA!=\"\" AND FieldB=\"I'm innocent\"");

        assertNotNull(formatString);
        assertEquals("FieldA!=\"\" and FieldB==\"I'm innocent\"", formatString);
    }

    @Test
    void formatEnablingConditionWithOrOperator() {
        String formatString = this.enablingConditionFormatter
            .convert("FieldA!=\"\" OR FieldB!=\"I'm innocent\"");

        assertNotNull(formatString);
        assertEquals("FieldA!=\"\" or FieldB!=\"I'm innocent\"", formatString);
    }

    @Test
    void formatEmptyEnablingCondition() {
        String formatString = this.enablingConditionFormatter.convert("");

        assertNotNull(formatString);
        assertEquals("", formatString);
    }

    @Test
    void formatNullEnablingCondition() {
        String formatString = this.enablingConditionFormatter.convert(null);
        assertNull(formatString);
    }

    @Test
    void formatEnablingConditionWithRegularExpression() {
        String formatString = this.enablingConditionFormatter
            .convert("FieldA!=\"*\" AND FieldB=\"I'm innocent\"");

        assertNotNull(formatString);
        assertEquals("FieldA!~\".*\" and FieldB==\"I'm innocent\"", formatString);
    }

    @Test
    void formatEnablingConditionWithEqualityRegularExpression() {
        String formatString = this.enablingConditionFormatter
            .convert("FieldA=\"*\" AND FieldB=\"I'm innocent\"");

        assertNotNull(formatString);
        assertEquals("FieldA=~\".*\" and FieldB==\"I'm innocent\"", formatString);
    }

}
