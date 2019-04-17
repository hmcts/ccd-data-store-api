package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.test.CaseFieldBuilder;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

@DisplayName("TextValidator")
class TextValidatorTest {
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";

    @Mock
    private BaseType textBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private TextValidator validator;
    private CaseField caseField;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(textBaseType.getType()).thenReturn(TextValidator.TYPE_ID);
        BaseType.register(textBaseType);

        validator = new TextValidator();

        caseField = caseField().withMin(5)
                               .withMax(10)
                               .build();
    }

    @Test
    @DisplayName("should be valid when text length between min and max")
    void textFieldWithValidMinMax() {
        final JsonNode DATA = NODE_FACTORY.textNode("5 & 10");
        final List<ValidationResult> validMinMaxResults = validator.validate(FIELD_ID, DATA, caseField);

        assertThat(validMinMaxResults.toString(), validMinMaxResults, hasSize(0));
    }

    @Test
    @DisplayName("should NOT be valid when text length outside of min and max")
    void textFieldWithInvalidMinMax() {
        final JsonNode INVALID_MIN = NODE_FACTORY.textNode("Test");
        final List<ValidationResult> validMinResults = validator.validate(FIELD_ID, INVALID_MIN, caseField);

        final JsonNode INVALID_MAX = NODE_FACTORY.textNode("Test Test Test");
        final List<ValidationResult> validMaxResults = validator.validate(FIELD_ID, INVALID_MAX, caseField);

        assertAll(
            () -> assertThat("Min not catched", validMinResults, hasSize(1)),
            () -> assertThat("Max not catched", validMaxResults, hasSize(1)),
            () -> assertThat(validMinResults, hasItem(
                hasProperty("errorMessage", equalTo("Test require minimum length 5")))),
            () -> assertThat(validMinResults, hasItem(hasProperty("fieldId", equalTo(FIELD_ID)))),
            () -> assertThat(validMaxResults, hasItem(
                hasProperty("errorMessage", equalTo("Test Test Test exceed maximum length 10")))),
            () -> assertThat(validMaxResults, hasItem(hasProperty("fieldId", equalTo(FIELD_ID))))
        );
    }

    @Test
    @DisplayName("should be valid when no min and max defined")
    void textFieldWithNoMinMax() {
        final CaseField caseField = caseField().build();
        final JsonNode value = NODE_FACTORY.textNode("Test");
        final List<ValidationResult> validMinMaxResults = validator.validate(FIELD_ID, value, caseField);
        assertThat(validMinMaxResults.toString(), validMinMaxResults, hasSize(0));
    }

    @Test
    @DisplayName("should test exact length when min and max are equal")
    void textFieldWithSameMinMax() {
        final CaseField caseField = caseField().withMin(5)
                                               .withMax(5)
                                               .build();
        final JsonNode valid_value = NODE_FACTORY.textNode("12345");
        final List<ValidationResult> validMinMaxResults = validator.validate(FIELD_ID, valid_value, caseField);

        // Test value over
        final JsonNode over_value = NODE_FACTORY.textNode("123456");
        final List<ValidationResult> overMinMaxResults = validator.validate(FIELD_ID, over_value, caseField);

        // Test value under
        final JsonNode under_value = NODE_FACTORY.textNode("1234");
        final List<ValidationResult> underMinMaxResults = validator.validate(FIELD_ID, under_value, caseField);

        assertAll(
            () -> assertThat("Expected valid input", validMinMaxResults, hasSize(0)),
            () -> assertThat("Under allowed minimum", underMinMaxResults, hasSize(1)),
            () -> assertThat("Over allowed maximum", overMinMaxResults, hasSize(1))
        );
    }

    @Test
    @DisplayName("should test against regular expression")
    void textRegex() {
        final CaseField caseField = caseField().withRegExp("\\d{4}-\\d{2}-\\d{2}").build();
        final JsonNode validValue = NODE_FACTORY.textNode("1234-56-78");
        final List<ValidationResult> validResult = validator.validate(FIELD_ID, validValue, caseField);

        final JsonNode invalidValue = NODE_FACTORY.textNode("aa-56-78");
        final List<ValidationResult> invalidResult = validator.validate(FIELD_ID, invalidValue, caseField);

        assertAll(
            () -> assertThat("Expected input to be valid", validResult, hasSize(0)),
            () -> assertThat("Expected input NOT to be valid", invalidResult, hasSize(1))
        );
    }

    @Test
    @DisplayName("should be linked to base type")
    void getType() {
        assertThat(validator.getType(), is(BaseType.get("Text")));
    }

    @Test
    @DisplayName("should be valid when input is null value")
    void nullValue() {
        final List<ValidationResult> validationResult = validator.validate(FIELD_ID, null, caseField);
        assertThat(validationResult, hasSize(0));
    }

    @Test
    @DisplayName("should be valid when input is null text node")
    void nullTextValue() {
        final List<ValidationResult> validationResult = validator.validate(FIELD_ID, new TextNode(null), caseField);
        assertThat(validationResult, hasSize(0));
    }

    @Test
    @DisplayName("should NOT be valid with only white spaces or leading trailing white spaces")
    void checkWhitespaces() {
        final JsonNode whitespaceValue = NODE_FACTORY.textNode("   ");
        final List<ValidationResult> whitespaceValueResult = validator.validate(FIELD_ID, whitespaceValue, caseField);
        assertThat(whitespaceValueResult, hasSize(1));

        final JsonNode trailingwhitespaces = NODE_FACTORY.textNode("   Testing   ");
        final List<ValidationResult> trailingwhitespacesResult = validator.validate(FIELD_ID, trailingwhitespaces, caseField);
        assertThat(trailingwhitespacesResult, hasSize(0));
    }

    private CaseFieldBuilder caseField() {
        return new CaseFieldBuilder(FIELD_ID).withType(TextValidator.TYPE_ID);
    }
}
