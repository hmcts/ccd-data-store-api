package uk.gov.hmcts.ccd.domain.enablingcondition.jexl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(formatString).isEqualTo("FieldA!=\"\" and FieldB==\"I'm innocent\"");
    }

    @Test
    void formatEnablingConditionWithOrOperator() {
        String formatString = this.enablingConditionFormatter
            .convert("FieldA!=\"\" OR FieldB!=\"I'm innocent\"");

        assertThat(formatString).isEqualTo("FieldA!=\"\" or FieldB!=\"I'm innocent\"");
    }

    @Test
    void formatEnablingConditionWithAdditionalWhitespaceAroundAndOperator() {
        String formatString = this.enablingConditionFormatter
            .convert("FieldA!=\"\"  AND  FieldB=\"I'm innocent\"");

        assertThat(formatString).isEqualTo("FieldA!=\"\" and FieldB==\"I'm innocent\"");
    }

    @Test
    void formatEnablingConditionWithTabsAroundOrOperator() {
        String formatString = this.enablingConditionFormatter
            .convert("FieldA!=\"\"\tOR\tFieldB!=\"I'm innocent\"");

        assertThat(formatString).isEqualTo("FieldA!=\"\" or FieldB!=\"I'm innocent\"");
    }

    @Test
    void formatEnablingConditionDoesNotSplitOnOperatorInsideQuotedValue() {
        String formatString = this.enablingConditionFormatter
            .convert("FieldA=\"A OR B\" AND FieldB=\"C AND D\"");

        assertThat(formatString).isEqualTo("FieldA==\"A OR B\" and FieldB==\"C AND D\"");
    }

    @Test
    void formatEnablingConditionDoesNotPartiallyParseNonOperatorText() {
        String enablingCondition = "FieldA=\"x\" BAND FieldB=\"y\"";

        String formatString = this.enablingConditionFormatter.convert(enablingCondition);

        assertThat(formatString).isEqualTo(enablingCondition);
    }

    @Test
    void formatEmptyEnablingCondition() {
        String formatString = this.enablingConditionFormatter.convert("");

        assertThat(formatString).isEmpty();
    }

    @Test
    void formatNullEnablingCondition() {
        String formatString = this.enablingConditionFormatter.convert(null);
        assertThat(formatString).isNull();
    }

    @Test
    void formatEnablingConditionWithRegularExpression() {
        String formatString = this.enablingConditionFormatter
            .convert("FieldA!=\"*\" AND FieldB=\"I'm innocent\"");

        assertThat(formatString).isEqualTo("FieldA!~\".*\" and FieldB==\"I'm innocent\"");
    }

    @Test
    void formatEnablingConditionWithEqualityRegularExpression() {
        String formatString = this.enablingConditionFormatter
            .convert("FieldA=\"*\" AND FieldB=\"I'm innocent\"");

        assertThat(formatString).isEqualTo("FieldA=~\".*\" and FieldB==\"I'm innocent\"");
    }

}
