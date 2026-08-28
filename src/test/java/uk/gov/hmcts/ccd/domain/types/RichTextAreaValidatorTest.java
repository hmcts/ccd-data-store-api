package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.test.CaseFieldDefinitionBuilder;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RichTextAreaValidator")
class RichTextAreaValidatorTest {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";
    private static final String[] ALLOWED_WHITELIST_TAGS = {"p", "br", "strong", "ul", "li", "hr"};


    private final RichTextAreaValidator validator = new RichTextAreaValidator();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(validator, "allowedWhitelistTags", ALLOWED_WHITELIST_TAGS);

        FieldTypeDefinition richTextAreaType = baseTypeDefinition();
        CaseDefinitionRepository definitionRepository = mock(CaseDefinitionRepository.class);

        when(definitionRepository.getBaseTypes()).thenReturn(List.of(richTextAreaType));
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.register(new BaseType(richTextAreaType));
    }

    @Test
    void getType() {
        assertEquals(BaseType.get("RichTextArea"), validator.getType());
    }

    @ParameterizedTest
    @MethodSource("validEmptyValues")
    void validateShouldBeValidWhenNullOrEmpty(JsonNode value) {
        assertTrue(validate(value, caseField().build()).isEmpty());
    }


    @ParameterizedTest
    @ValueSource(strings = {"<p><strong>Order</strong></p>",
        "<p>Order</p>",
        "<p>Order</p><p>Order</p>",
        "<p>Order</p><br><p>Order</p>",
        "<p>a<br>b</p>",
        "<p>x</p><hr>",
        "<hr>",
        "<ul><li>a</li></ul><br>"})
    void validateShouldBeValidWhenMinimumLengthRequirementMet(String value) {
        assertTrue(validate(NODE_FACTORY.textNode(value), caseField().withMin(4).build()).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"<z><invalid text></z>", "some test", "<", "/>", "</>" })
    void validateInvalidValuesShouldFailValidation(String value) {
        JsonNode node = NODE_FACTORY.textNode(value);
        CaseFieldDefinition field = caseField().withMin(1).build();
        assertThatThrownBy(() -> validate(node, field)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateShouldNotBeValidWhenMinimumLengthRequirementNotMet() {
        List<ValidationResult> results = validate(NODE_FACTORY.textNode("<p>m</p>"),
            caseField().withMin(10).build());

        assertSingleError(results, "requires a minimum length of 10");
    }

    @Test
    void validateShouldIgnoreMaximumLengthAndRegexRequirements() {
        CaseFieldDefinition constrainedField = caseField()
            .withMax(4)
            .withRegExp("\\d{4}-\\d{2}-\\d{2}")
            .build();
        JsonNode node = NODE_FACTORY.textNode("not-a-date-and-longer-than-four");
        assertThatThrownBy(() -> validate(node, constrainedField)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateShouldBeInvalidWhenValueProvidedIsNotText() {
        List<ValidationResult> results = validate(NODE_FACTORY.numberNode(2), caseField().build());

        assertSingleError(results, "number is not a string");
    }

    private static Stream<JsonNode> validEmptyValues() {
        return Stream.of(null, NODE_FACTORY.nullNode(), NODE_FACTORY.textNode(""));
    }

    private List<ValidationResult> validate(JsonNode value, CaseFieldDefinition fieldDefinition) {
        return validator.validate(FIELD_ID, value, fieldDefinition);
    }

    private static CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(RichTextAreaValidator.TYPE_ID);
    }

    private static FieldTypeDefinition baseTypeDefinition() {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setType(RichTextAreaValidator.TYPE_ID);
        return fieldTypeDefinition;
    }

    private static void assertSingleError(List<ValidationResult> results, String errorMessage) {
        assertEquals(1, results.size());
        assertEquals(FIELD_ID, results.get(0).getFieldId());
        assertEquals(errorMessage, results.get(0).getErrorMessage());
    }
}
