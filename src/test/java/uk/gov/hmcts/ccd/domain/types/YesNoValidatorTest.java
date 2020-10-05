package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@DisplayName("YesNoValidator")
class YesNoValidatorTest {
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";

    @Mock
    private BaseType yesNoBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private YesNoValidator validator;
    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(yesNoBaseType.getType()).thenReturn(YesNoValidator.TYPE_ID);
        BaseType.register(yesNoBaseType);

        validator = new YesNoValidator();

        caseFieldDefinition = caseField().build();
    }

    @Test
    void correctValue() {
        final JsonNode upperYes = NODE_FACTORY.textNode("YES");
        final JsonNode lowerYes = NODE_FACTORY.textNode("yes");
        final JsonNode upperNo = NODE_FACTORY.textNode("NO");
        final JsonNode lowerNo = NODE_FACTORY.textNode("no");

        assertAll(
            () -> assertEquals(0, validator.validate(FIELD_ID, upperYes, caseFieldDefinition).size(),
                "YES should be valid"),
            () -> assertEquals(0, validator.validate(FIELD_ID, upperNo, caseFieldDefinition).size(),
                "NO should be valid"),
            () -> assertEquals(0, validator.validate(FIELD_ID, lowerYes, caseFieldDefinition).size(),
                "yes should be valid"),
            () -> assertEquals(0, validator.validate(FIELD_ID, lowerNo, caseFieldDefinition).size(),
                "no should be valid")
        );
    }

    @Test
    void incorrectValue() {
        final JsonNode anything = NODE_FACTORY.textNode("dasdahsaAAA");
        assertEquals(1, validator.validate(FIELD_ID, anything, caseFieldDefinition).size(),
            "Did not catch non YES/NO");
    }

    @Test
    void nullValue() {
        assertEquals(0, validator.validate(FIELD_ID, null, caseFieldDefinition).size(), "Did not catch NULL");
    }

    @Test
    void nullNode() {
        final JsonNode nullNode = NODE_FACTORY.nullNode();
        assertEquals(0, validator.validate(FIELD_ID, nullNode, caseFieldDefinition).size(), "Did not catch NULL");
    }

    @Test
    void shouldFail_whenValidatingBooleanNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.booleanNode(true), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("true is not " + YesNoValidator.TYPE_ID));
    }

    @Test
    void shouldFail_whenValidatingArrayNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.arrayNode(), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("[] is not " + YesNoValidator.TYPE_ID));
    }

    @Test
    void shouldFail_whenValidatingBinaryNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.binaryNode("Yes".getBytes()), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), endsWith(" is not " + YesNoValidator.TYPE_ID));
    }

    @Test
    void shouldPass_whenValidatingObjectNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.objectNode(), caseFieldDefinition);
        assertThat(result, empty());
    }

    @Test
    void shouldPass_whenValidatingPojoNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.pojoNode("Yes"), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is(NODE_FACTORY.pojoNode("Yes") + " is not "
            + YesNoValidator.TYPE_ID));
    }

    @Test
    void shouldPass_whenValidatingNumberNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.numberNode(1), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("1 is not " + YesNoValidator.TYPE_ID));
    }

    @Test
    void shouldPass_whenValidatingNullNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.nullNode(), caseFieldDefinition);
        assertThat(result, empty());
    }

    @Test
    void shouldPass_whenValidatingNullValue() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(null), caseFieldDefinition);
        assertThat(result, empty());
    }

    @Test
    void getType() {
        assertEquals(validator.getType(), BaseType.get("YesOrNo"));
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(YesNoValidator.TYPE_ID);
    }
}
