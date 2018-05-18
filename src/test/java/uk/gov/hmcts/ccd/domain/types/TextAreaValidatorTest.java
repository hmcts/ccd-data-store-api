package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class TextAreaValidatorTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final CaseField CASE_FIELD = new CaseField();
    private static final String FIELD_ID = "AdditionalNote";

    static {
        CASE_FIELD.setId(FIELD_ID);
    }

    private TextAreaValidator validator;
    private FieldType fieldType;

    @Before
    public void setUp() throws Exception {
        validator = new TextAreaValidator();

        fieldType = new FieldType();
        fieldType.setId(TextAreaValidator.TYPE_ID);
        fieldType.setType(TextAreaValidator.TYPE_ID);

        CASE_FIELD.setFieldType(fieldType);
    }

    @org.junit.Ignore("RDM-2190 investigate why this unit case failed on BaseType#L32")
    @Test
    public void getType() {
        assertThat(validator.getType(), is(BaseType.get("TextArea")));
    }

    @Test
    public void validate_shouldBeValidWhenNull() {
        List<ValidationResult> results = validator.validate(FIELD_ID, null, CASE_FIELD);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));

        results = validator.validate(FIELD_ID, JSON_NODE_FACTORY.nullNode(), CASE_FIELD);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldBeValidWhenEmptyString() {
        final List<ValidationResult> results = validator.validate(FIELD_ID, JSON_NODE_FACTORY.textNode(""), CASE_FIELD);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldBeValidWhenNonEmptyString() {
        final List<ValidationResult> results = validator.validate(FIELD_ID, JSON_NODE_FACTORY.textNode("Some text"), CASE_FIELD);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldNotBeValidWhenMinimumLengthRequirementNotMet() {
        fieldType.setMin(new BigDecimal(4));

        final List<ValidationResult> results = validator.validate(FIELD_ID, JSON_NODE_FACTORY.textNode("xxx"), CASE_FIELD);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getFieldId(), equalTo(FIELD_ID));
    }

    @Test
    public void validate_shouldBeValidWhenMinimumLengthRequirementMet() {
        fieldType.setMin(new BigDecimal(4));

        final List<ValidationResult> results = validator.validate(FIELD_ID, JSON_NODE_FACTORY.textNode("xxx4"), CASE_FIELD);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldNotBeValidWhenMaximumLengthRequirementNotMet() {
        fieldType.setMax(new BigDecimal(4));

        final List<ValidationResult> results = validator.validate(FIELD_ID, JSON_NODE_FACTORY.textNode("xxx45"), CASE_FIELD);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getFieldId(), equalTo(FIELD_ID));
    }

    @Test
    public void validate_shouldBeValidWhenMaximumLengthRequirementMet() {
        fieldType.setMax(new BigDecimal(4));

        final List<ValidationResult> results = validator.validate(FIELD_ID, JSON_NODE_FACTORY.textNode("xxx"), CASE_FIELD);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldNotBeValidWhenRegexRequirementNotMet() {
        fieldType.setRegularExpression("\\d{4}-\\d{2}-\\d{2}");

        final List<ValidationResult> results = validator.validate(FIELD_ID, JSON_NODE_FACTORY.textNode("3232"), CASE_FIELD);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getFieldId(), equalTo(FIELD_ID));
    }

    @Test
    public void validate_shouldBeValidWhenRegexRequirementMet() {
        fieldType.setRegularExpression("\\d{4}-\\d{2}-\\d{2}");

        final List<ValidationResult> results = validator.validate(FIELD_ID, JSON_NODE_FACTORY.textNode("3232-32-32"), CASE_FIELD);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldBeInvalidWhenValueProvidedIsNotText() {
        final List<ValidationResult> results = validator.validate(FIELD_ID, JSON_NODE_FACTORY.numberNode(2), CASE_FIELD);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getFieldId(), equalTo(FIELD_ID));
    }
}
