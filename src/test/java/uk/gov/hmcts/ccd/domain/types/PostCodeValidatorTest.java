package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.types.PostCodeValidator.TYPE_ID;

@DisplayName("PostcodeValidator")
class PostCodeValidatorTest {
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";
    private static final String POSTCODE_REGEX = "^([A-PR-UWYZ0-9][A-HK-Y0-9][AEHMNPRTVXY0-9]?[ABEHMNPRVWXY0-9]? {1,2}[0-9][ABD-HJLN-UW-Z]{2}|GIR 0AA)$";

    @Mock
    private BaseType postcodeBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private PostCodeValidator validator;
    private CaseField caseField;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(postcodeBaseType.getType()).thenReturn(PostCodeValidator.TYPE_ID);
        when(postcodeBaseType.getRegularExpression()).thenReturn(POSTCODE_REGEX);
        BaseType.register(postcodeBaseType);

        validator = new PostCodeValidator();

        caseField = caseField().build();
    }

    @Test
    void validPostCodesForBaseRegEx() {
        final List<ValidationResult> result01 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("M1 1AA"),
                                                                   caseField);
        final List<ValidationResult> result02 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("N60 1NW"),
                                                                   caseField);
        final List<ValidationResult> result03 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("CR2 6XH"),
                                                                   caseField);
        final List<ValidationResult> result04 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("DN55 1PT"),
                                                                   caseField);
        final List<ValidationResult> result05 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("W1A 1HQ"),
                                                                   caseField);
        final List<ValidationResult> result06 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("EC1A 1BB"),
                                                                   caseField);

        assertAll(
            () -> assertEquals(0, result01.size(), result01.toString()),
            () -> assertEquals(0, result02.size(), result02.toString()),
            () -> assertEquals(0, result03.size(), result01.toString()),
            () -> assertEquals(0, result04.size(), result04.toString()),
            () -> assertEquals(0, result05.size(), result05.toString()),
            () -> assertEquals(0, result06.size(), result06.toString())
        );
    }

    @Test
    void invalidPostCodesForBaseRegEx() {
        final List<ValidationResult> result01 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("3321M1 1AA"),
                                                                   caseField);
        final List<ValidationResult> result02 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("1m1 1m1"),
                                                                   caseField);

        assertAll(
            () -> assertEquals(1, result01.size(), result01.toString()),
            () -> assertEquals(1, result01.size(), result02.toString())
        );
    }


    @Test
    void checkFieldRegex() {
        final CaseField caseField = caseField().withRegExp("^[0-9]*$").build();
        final List<ValidationResult> validResult = validator.validate(FIELD_ID,
                                                                      NODE_FACTORY.textNode("123456789"),
                                                                      caseField);
        assertEquals(0, validResult.size());

        final List<ValidationResult> invalidResult = validator.validate(FIELD_ID,
                                                                        NODE_FACTORY.textNode("abc123"),
                                                                        caseField);
        assertEquals(1, invalidResult.size(), invalidResult.toString());
    }

    @Test
    void nullValue() {
        assertEquals(0, validator.validate(FIELD_ID, null, caseField).size());
    }

    @Test
    void getType() {
        assertEquals(validator.getType(), BaseType.get("POSTCODE"));
    }

    @Test
    void testInvalidMin() {
        final CaseField caseField = caseField().withMin(5).build();
        final JsonNode INVALID_MIN = NODE_FACTORY.textNode("Test");
        final List<ValidationResult> validationResults = validator.validate(FIELD_ID, INVALID_MIN, caseField);
        assertEquals(1, validationResults.size(), "Did not catch min");
        assertEquals("Post code 'Test' requires minimum length 5", validationResults.get(0).getErrorMessage());
        assertEquals(FIELD_ID, validationResults.get(0).getFieldId());
    }

    @Test
    void testInvalidMax() {
        final CaseField caseField = caseField().withMax(6).build();
        final JsonNode INVALID_MAX = NODE_FACTORY.textNode("Test Test Test");
        final List<ValidationResult> validationResults = validator.validate(FIELD_ID, INVALID_MAX, caseField);
        assertEquals(1, validationResults.size(), "Did not catch max");
        assertEquals("Post code 'Test Test Test' exceeds maximum length 6", validationResults.get(0).getErrorMessage());
        assertEquals(FIELD_ID, validationResults.get(0).getFieldId());
    }

    @Test
    void testValidMinMaxButNoRegExChecks() {
        final CaseField caseField = caseField().withMin(5)
                                               .withMax(6)
                                               .build();
        // Disable regular expression checks
        when(postcodeBaseType.getRegularExpression()).thenReturn("^.*$");

        final JsonNode DATA = NODE_FACTORY.textNode("5 & 10");
        final List<ValidationResult> validMinMaxResults = validator.validate(FIELD_ID, DATA, caseField);
        assertEquals(0, validMinMaxResults.size(), validMinMaxResults.toString());
    }

    @Test
    void shouldFail_whenValidatingBinaryNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.binaryNode("EC1A 1BB".getBytes()), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), endsWith(" needs to be a valid " + TYPE_ID));
    }

    @Test
    void shouldPass_whenValidatingObjectNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.objectNode(), caseField);
        assertThat(result, empty());
    }

    @Test
    void shouldFail_whenValidatingArrayNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.arrayNode(), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("[] needs to be a valid " + TYPE_ID));
    }

    @Test
    void shouldFail_whenValidatingNumberNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.numberNode(1), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("1 needs to be a valid " + TYPE_ID));
    }

    @Test
    void shouldFail_whenValidatingBooleanNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.booleanNode(true), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("true needs to be a valid " + TYPE_ID));
    }

    @Test
    void shouldFail_whenValidatingPojoNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.pojoNode("EC1A 1BB"), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is(NODE_FACTORY.pojoNode("EC1A 1BB") + " needs to be a valid " + TYPE_ID));
    }

    @Test
    void shouldPass_whenValidatingNullNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.nullNode(), caseField);
        assertThat(result, empty());
    }

    @Test
    void shouldPass_whenValidatingNulText() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(null), caseField);
        assertThat(result, empty());
    }

    private CaseFieldBuilder caseField() {
        return new CaseFieldBuilder(FIELD_ID).withType(PostCodeValidator.TYPE_ID);
    }
}
