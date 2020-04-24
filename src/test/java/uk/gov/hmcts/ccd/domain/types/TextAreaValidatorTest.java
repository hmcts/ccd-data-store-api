package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.test.CaseFieldDefinitionBuilder;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@DisplayName("TextAreaValidator")
class TextAreaValidatorTest {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";

    @Mock
    private BaseType textAreaBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private TextAreaValidator validator;
    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(textAreaBaseType.getType()).thenReturn(TextAreaValidator.TYPE_ID);
        BaseType.register(textAreaBaseType);

        validator = new TextAreaValidator();

        caseFieldDefinition = caseField().build();
    }

    @Test
    void getType() {
        assertThat(validator.getType(), is(BaseType.get("TextArea")));
    }

    @Test
    void validate_shouldBeValidWhenNull() {
        List<ValidationResult> results = validator.validate(FIELD_ID, null, caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));

        results = validator.validate(FIELD_ID, NODE_FACTORY.nullNode(), caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldBeValidWhenEmptyString() {
        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.textNode(""), caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldBeValidWhenNonEmptyString() {
        final List<ValidationResult> results = validator.validate(FIELD_ID,
                                                                  NODE_FACTORY.textNode("Some text"),
                caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldNotBeValidWhenMinimumLengthRequirementNotMet() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(4).build();

        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.textNode("xxx"), caseFieldDefinition);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getFieldId(), equalTo(FIELD_ID));
    }

    @Test
    void validate_shouldBeValidWhenMinimumLengthRequirementMet() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(4).build();

        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.textNode("xxx4"), caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldNotBeValidWhenMaximumLengthRequirementNotMet() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMax(4).build();

        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.textNode("xxx45"), caseFieldDefinition);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getFieldId(), equalTo(FIELD_ID));
    }

    @Test
    void validate_shouldBeValidWhenMaximumLengthRequirementMet() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMax(4).build();

        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.textNode("xxx"), caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldNotBeValidWhenRegexRequirementNotMet() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withRegExp("\\d{4}-\\d{2}-\\d{2}").build();

        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.textNode("3232"), caseFieldDefinition);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getFieldId(), equalTo(FIELD_ID));
    }

    @Test
    void validate_shouldBeValidWhenRegexRequirementMet() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withRegExp("\\d{4}-\\d{2}-\\d{2}").build();

        final List<ValidationResult> results = validator.validate(FIELD_ID,
                                                                  NODE_FACTORY.textNode("3232-32-32"),
                caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldBeInvalidWhenValueProvidedIsNotText() {
        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.numberNode(2), caseFieldDefinition);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getFieldId(), equalTo(FIELD_ID));
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(TextAreaValidator.TYPE_ID);
    }
}
