package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.FixedListItem;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static java.math.BigDecimal.ONE;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class MultiSelectListValidatorTest {

    private static final JsonNodeFactory NODE_FACTORY = new JsonNodeFactory(Boolean.FALSE);
    private static final String FIELD_ID = "MultiSelectList1";
    private static final String OPTION_1 = "Option1";
    private static final String OPTION_2 = "Option2";
    private static final String OPTION_3 = "Option3";
    private static final String OPTION_UNKNOWN = "OptionUnknown";

    private CaseField caseField;
    private FieldType fieldType;

    private MultiSelectListValidator validator;

    @Before
    public void setUp() throws Exception {
        final FixedListItem option1 = new FixedListItem();
        option1.setCode(OPTION_1);
        final FixedListItem option2 = new FixedListItem();
        option2.setCode(OPTION_2);
        final FixedListItem option3 = new FixedListItem();
        option3.setCode(OPTION_3);

        fieldType = new FieldType();
        fieldType.setFixedListItems(Arrays.asList(option1, option2, option3));

        caseField = new CaseField();
        caseField.setFieldType(fieldType);

        validator = new MultiSelectListValidator();
    }

    @Test
    public void getType() {
        assertThat(validator.getType(), is(BaseType.get("MultiSelectList")));
    }

    @Test
    public void validate_shouldBeValidWhenNull() {
        final List<ValidationResult> results = validator.validate(FIELD_ID, null, caseField);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldBeValidWhenNullNode() {
        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.nullNode(), caseField);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldBeValidWhenArrayOfValidValues() {
        final ArrayNode values = NODE_FACTORY.arrayNode()
            .add(OPTION_1)
            .add(OPTION_2);

        final List<ValidationResult> results = validator.validate(FIELD_ID, values, caseField);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldNOTBeValidWhenValueIsNotAnArray() {
        final JsonNode value = NODE_FACTORY.textNode("Nayab was here, 24/07/2017");

        final List<ValidationResult> results = validator.validate(FIELD_ID, value, caseField);

        assertThat(results, hasSize(1));
    }

    @Test
    public void validate_shouldNOTBeValidWhenContainsUnknwonValue() {
        final ArrayNode values = NODE_FACTORY.arrayNode()
            .add(OPTION_1)
            .add(OPTION_UNKNOWN);

        final List<ValidationResult> results = validator.validate(FIELD_ID, values, caseField);

        assertThat(results, hasSize(1));
    }

    @Test
    public void validate_shouldNOTBeValidWhenContainsDuplicateValues() {
        final ArrayNode values = NODE_FACTORY.arrayNode()
            .add(OPTION_1)
            .add(OPTION_1);

        final List<ValidationResult> results = validator.validate(FIELD_ID, values, caseField);

        assertThat(results, hasSize(1));
    }

    @Test
    public void validate_shouldNOTBeValidWhenBelowMin() {
        fieldType.setMin(new BigDecimal(2));

        final ArrayNode values = NODE_FACTORY.arrayNode()
            .add(OPTION_1);

        final List<ValidationResult> results = validator.validate(FIELD_ID, values, caseField);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getErrorMessage(), equalTo("Select at least 2 options"));
    }

    @Test
    public void validate_shouldNOTBeValidWhenAboveMax() {
        fieldType.setMax(ONE);

        final ArrayNode values = NODE_FACTORY.arrayNode()
            .add(OPTION_1)
            .add(OPTION_2);

        final List<ValidationResult> results = validator.validate(FIELD_ID, values, caseField);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getErrorMessage(), equalTo("Cannot select more than 1 option"));
    }

}
