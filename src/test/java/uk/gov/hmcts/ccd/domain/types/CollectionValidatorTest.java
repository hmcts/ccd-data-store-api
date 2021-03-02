package uk.gov.hmcts.ccd.domain.types;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ONE;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

public class CollectionValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TYPE_TEXT = "Text";
    private static final String CASE_FIELD_ID = "Aliases";

    private CollectionValidator validator;
    private CaseFieldDefinition caseFieldDefinition;

    @Before
    public void setUp() throws Exception {
        validator = new CollectionValidator();

        final FieldTypeDefinition collectionFieldTypeDefinition = new FieldTypeDefinition();
        collectionFieldTypeDefinition.setId(TYPE_TEXT);
        collectionFieldTypeDefinition.setType(TYPE_TEXT);

        final FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setId(TYPE_TEXT);
        fieldTypeDefinition.setType(COLLECTION);
        fieldTypeDefinition.setCollectionFieldTypeDefinition(collectionFieldTypeDefinition);

        caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setId(CASE_FIELD_ID);
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);
    }

    @Test
    public void validate_emptyShouldBeValid() throws IOException {
        final List<ValidationResult> results = validator.validate(CASE_FIELD_ID, MAPPER.readTree("[]"),
            caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_invalidMin() throws IOException {
        caseFieldDefinition.getFieldTypeDefinition().setMin(new BigDecimal(2));

        final List<ValidationResult> results =
            validator.validate(CASE_FIELD_ID, MAPPER.readTree("[ { \"value\": \"V1\"} ]"), caseFieldDefinition);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getErrorMessage(), equalTo("Add at least 2 values"));
    }

    @Test
    public void validate_invalidMax() throws IOException {
        caseFieldDefinition.getFieldTypeDefinition().setMax(ONE);

        final List<ValidationResult> results = validator.validate(CASE_FIELD_ID, MAPPER.readTree(
            "[ { \"value\": \"V1\"}, { \"value\": \"V2\"} ]"),
            caseFieldDefinition);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getErrorMessage(), equalTo("Cannot add more than 1 value"));
    }

    @Test
    public void validate_validMinMax() throws IOException {
        caseFieldDefinition.getFieldTypeDefinition().setMin(ONE);
        caseFieldDefinition.getFieldTypeDefinition().setMax(ONE);

        final List<ValidationResult> results =
            validator.validate(CASE_FIELD_ID, MAPPER.readTree("[ { \"value\": \"V1\"} ]"), caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldBeInvalidWhenValueNotArray() throws IOException {
        final List<ValidationResult> results =
            validator.validate(CASE_FIELD_ID, MAPPER.readTree("\"Some text\""), caseFieldDefinition);

        assertThat(results, hasSize(1));
    }

    @Test
    public void validate_shouldBeValidWhenIDsUnique() throws IOException {
        final List<ValidationResult> results =
            validator.validate(CASE_FIELD_ID, MAPPER.readTree(
                "[ { \"id\": \"1\", \"value\": \"V1\"}, { \"id\": \"2\", \"value\": \"V2\"} ]"),
                caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldBeInvalidWhenMultipleItemsHaveSameID() throws IOException {
        final List<ValidationResult> results =
            validator.validate(CASE_FIELD_ID, MAPPER.readTree(
                "[ { \"id\": \"1\", \"value\": \"V1\"}, { \"id\": \"1\", \"value\": \"V2\"} ]"),
                caseFieldDefinition);

        assertThat(results, hasSize(1));
    }

    @Test
    public void validate_shouldBeInvalidWhenInvalidIDType() throws IOException {
        final List<ValidationResult> results =
            validator.validate(CASE_FIELD_ID,
                MAPPER.readTree("[ { \"id\": 1, \"value\": \"V1\"} ]"), caseFieldDefinition);

        assertThat(results, hasSize(1));
    }

    @Test
    public void validate_shouldBeInvalidWhenAnItemIsMissingValue() throws IOException {
        final List<ValidationResult> results =
            validator.validate(CASE_FIELD_ID,
                MAPPER.readTree("[ { \"id\": \"1\", \"value\": \"V1\"}, { \"id\": \"2\"} ]"), caseFieldDefinition);

        assertThat(results, hasSize(1));
    }

}
