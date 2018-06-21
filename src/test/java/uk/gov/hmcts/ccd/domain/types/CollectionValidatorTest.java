package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ONE;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class CollectionValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TYPE_TEXT = "Text";
    private static final String TYPE_COLLECTION = "Collection";
    private static final String CASE_FIELD_ID = "Aliases";

    private CollectionValidator validator;
    private CaseField caseField;

    @Before
    public void setUp() throws Exception {
        validator = new CollectionValidator();

        final FieldType collectionFieldType = new FieldType();
        collectionFieldType.setId(TYPE_TEXT);
        collectionFieldType.setType(TYPE_TEXT);

        final FieldType fieldType = new FieldType();
        fieldType.setId(TYPE_TEXT);
        fieldType.setType(TYPE_COLLECTION);
        fieldType.setCollectionFieldType(collectionFieldType);

        caseField = new CaseField();
        caseField.setId(CASE_FIELD_ID);
        caseField.setFieldType(fieldType);
    }

    @Test
    public void validate_emptyShouldBeValid() throws IOException {
        final List<ValidationResult> results = validator.validate(CASE_FIELD_ID, MAPPER.readTree("[]"), caseField);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_invalidMin() throws IOException {
        caseField.getFieldType().setMin(new BigDecimal(2));

        final List<ValidationResult> results = validator.validate(CASE_FIELD_ID, MAPPER.readTree("[ { \"value\": \"V1\"} ]"), caseField);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getErrorMessage(), equalTo("Add at least 2 values"));
    }

    @Test
    public void validate_invalidMax() throws IOException {
        caseField.getFieldType().setMax(ONE);

        final List<ValidationResult> results = validator.validate(CASE_FIELD_ID, MAPPER.readTree("[ { \"value\": \"V1\"}, { \"value\": \"V2\"} ]"), caseField);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getErrorMessage(), equalTo("Cannot add more than 1 value"));
    }

    @Test
    public void validate_validMinMax() throws IOException {
        caseField.getFieldType().setMin(ONE);
        caseField.getFieldType().setMax(ONE);

        final List<ValidationResult> results = validator.validate(CASE_FIELD_ID, MAPPER.readTree("[ { \"value\": \"V1\"} ]"), caseField);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldBeInvalidWhenValueNotArray() throws IOException {
        final List<ValidationResult> results = validator.validate(CASE_FIELD_ID, MAPPER.readTree("\"Some text\""), caseField);

        assertThat(results, hasSize(1));
    }

    @Test
    public void validate_shouldBeValidWhenIDsUnique() throws IOException {
        final List<ValidationResult> results = validator.validate(CASE_FIELD_ID, MAPPER.readTree("[ { \"id\": \"1\", \"value\": \"V1\"}, { \"id\": \"2\", \"value\": \"V2\"} ]"), caseField);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    public void validate_shouldBeInvalidWhenMultipleItemsHaveSameID() throws IOException {
        final List<ValidationResult> results = validator.validate(CASE_FIELD_ID, MAPPER.readTree("[ { \"id\": \"1\", \"value\": \"V1\"}, { \"id\": \"1\", \"value\": \"V2\"} ]"), caseField);

        assertThat(results, hasSize(1));
    }

    @Test
    public void validate_shouldBeInvalidWhenInvalidIDType() throws IOException {
        final List<ValidationResult> results = validator.validate(CASE_FIELD_ID, MAPPER.readTree("[ { \"id\": 1, \"value\": \"V1\"} ]"), caseField);

        assertThat(results, hasSize(1));
    }

    @Test
    public void validate_shouldBeInvalidWhenAnItemIsMissingValue() throws IOException {
        final List<ValidationResult> results = validator.validate(CASE_FIELD_ID, MAPPER.readTree("[ { \"id\": \"1\", \"value\": \"V1\"}, { \"id\": \"2\"} ]"), caseField);

        assertThat(results, hasSize(1));
    }

}
