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
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@DisplayName("RichTextAreaValidator")
class RichTextAreaValidatorTest {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";

    @Mock
    private BaseType richTextAreaBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private RichTextAreaValidator validator;
    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(richTextAreaBaseType.getType()).thenReturn(RichTextAreaValidator.TYPE_ID);
        BaseType.register(richTextAreaBaseType);

        validator = new RichTextAreaValidator();

        caseFieldDefinition = caseField().build();
    }

    @Test
    void getType() {
        assertThat(validator.getType(), is(BaseType.get("RichTextArea")));
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
        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.textNode(""),
            caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldBeValidWhenStringContainsMarkup() {
        final List<ValidationResult> results = validator.validate(FIELD_ID,
            NODE_FACTORY.textNode("<p><strong>Order</strong></p>"), caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldNotBeValidWhenMinimumLengthRequirementNotMet() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(4).build();

        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.textNode("xxx"),
            caseFieldDefinition);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getFieldId(), equalTo(FIELD_ID));
        assertThat(results.get(0).getErrorMessage(), equalTo("requires a minimum length of 4"));
    }

    @Test
    void validate_shouldBeValidWhenMinimumLengthRequirementMet() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(4).build();

        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.textNode("xxx4"),
            caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldIgnoreMaximumLengthRequirement() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMax(4).build();

        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.textNode("xxx45"),
            caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldIgnoreRegexRequirement() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withRegExp("\\d{4}-\\d{2}-\\d{2}").build();

        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.textNode("not-a-date"),
            caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldBeInvalidWhenValueProvidedIsNotText() {
        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.numberNode(2),
            caseFieldDefinition);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getFieldId(), equalTo(FIELD_ID));
        assertThat(results.get(0).getErrorMessage(), equalTo("number is not a string"));
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(RichTextAreaValidator.TYPE_ID);
    }
}
