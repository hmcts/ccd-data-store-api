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
    private CaseField caseField;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(yesNoBaseType.getType()).thenReturn(YesNoValidator.TYPE_ID);
        BaseType.register(yesNoBaseType);

        validator = new YesNoValidator();

        caseField = caseField().build();
    }

    @Test
    void correctValue() {
        final JsonNode UPPER_YES = NODE_FACTORY.textNode("YES");
        final JsonNode LOWER_YES = NODE_FACTORY.textNode("yes");
        final JsonNode UPPER_NO = NODE_FACTORY.textNode("NO");
        final JsonNode LOWER_NO = NODE_FACTORY.textNode("no");

        assertAll(
            () -> assertEquals(0, validator.validate(FIELD_ID, UPPER_YES, caseField).size(), "YES should be valid"),
            () -> assertEquals(0, validator.validate(FIELD_ID, UPPER_NO, caseField).size(), "NO should be valid"),
            () -> assertEquals(0, validator.validate(FIELD_ID, LOWER_YES, caseField).size(), "yes should be valid"),
            () -> assertEquals(0, validator.validate(FIELD_ID, LOWER_NO, caseField).size(), "no should be valid")
        );
    }

    @Test
    void incorrectValue() {
        final JsonNode anything = NODE_FACTORY.textNode("dasdahsaAAA");
        assertEquals(1, validator.validate(FIELD_ID, anything, caseField).size(), "Did not catch non YES/NO");
    }

    @Test
    void nullValue() {
        assertEquals(0, validator.validate(FIELD_ID, null, caseField).size(), "Did not catch NULL");
    }

    @Test
    void nullNode() {
        final JsonNode nullNode = NODE_FACTORY.nullNode();
        assertEquals(0, validator.validate(FIELD_ID, nullNode, caseField).size(), "Did not catch NULL");
    }

    @Test
    void shouldFail_whenValidatingBooleanNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.booleanNode(true), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("true is not " + YesNoValidator.TYPE_ID));
    }

    @Test
    void shouldFail_whenValidatingArrayNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.arrayNode(), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("[] is not " + YesNoValidator.TYPE_ID));
    }

    @Test
    void shouldFail_whenValidatingBinaryNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.binaryNode("Yes".getBytes()), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), endsWith(" is not " + YesNoValidator.TYPE_ID));
    }

    @Test
    void shouldPass_whenValidatingObjectNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.objectNode(), caseField);
        assertThat(result, empty());
    }

    @Test
    void shouldPass_whenValidatingPojoNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.pojoNode("Yes"), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is(NODE_FACTORY.pojoNode("Yes") + " is not "
            + YesNoValidator.TYPE_ID));
    }

    @Test
    void shouldPass_whenValidatingNumberNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.numberNode(1), caseField);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("1 is not " + YesNoValidator.TYPE_ID));
    }

    @Test
    void shouldPass_whenValidatingNullNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.nullNode(), caseField);
        assertThat(result, empty());
    }

    @Test
    void shouldPass_whenValidatingNullValue() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(null), caseField);
        assertThat(result, empty());
    }

    @Test
    void getType() {
        assertEquals(validator.getType(), BaseType.get("YesOrNo"));
    }

    private CaseFieldBuilder caseField() {
        return new CaseFieldBuilder(FIELD_ID).withType(YesNoValidator.TYPE_ID);
    }
}
