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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.types.BaseTypeValidator.REGEX_GUIDANCE;

@DisplayName("NumberValidator")
class NumberValidatorTest {
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";

    @Mock
    private BaseType numberBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private NumberValidator validator;
    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(numberBaseType.getType()).thenReturn(NumberValidator.TYPE_ID);
        BaseType.register(numberBaseType);

        validator = new NumberValidator();

        caseFieldDefinition = caseField().build();
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(NumberValidator.TYPE_ID);
    }

    @Test
    void noValueOrMaxOrMin() {
        final JsonNode data = NODE_FACTORY.textNode("");
        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", data, caseFieldDefinition).size(),
                     validator.validate("TEST_FIELD_ID", data, caseFieldDefinition).toString());
    }

    @Test
    void noValueWithMaxOrMin() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(5)
                                               .withMax(10)
                                               .build();
        final JsonNode data = NODE_FACTORY.textNode("");
        assertEquals(0, validator.validate("TEST_FIELD_ID", data, caseFieldDefinition).size());
    }

    @Test
    void valueWithMaxMin() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(5)
                                               .withMax(10)
                                               .build();

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("5"), caseFieldDefinition).size(),
                     "5 should be with in range of between 5 and 10");

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(5), caseFieldDefinition).size(),
                     "5 should be with in range of between 5 and 10");

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("5.001"), caseFieldDefinition).size(),
                     "5.001 should be with in range of between 5 and 10");

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(5.001), caseFieldDefinition).size(),
                     "5.001 should be with in range of between 5 and 10");

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("9.999999"), caseFieldDefinition).size(),
                     "9.999999 should be with in range of between 5 and 10");

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(9.999999), caseFieldDefinition).size(),
                     "9.999999 should be with in range of between 5 and 10");

        List<ValidationResult> textNodeBelowMin = validator.validate("TEST_FIELD_ID",
                                                                     NODE_FACTORY.textNode("4.9"),
                caseFieldDefinition);
        assertEquals(1, textNodeBelowMin.size(), "4.9 should not be with in range of between 5 and 10");
        assertEquals("Should be more than or equal to 5", textNodeBelowMin.get(0).getErrorMessage());

        List<ValidationResult> numberNodeBelowMin = validator.validate("TEST_FIELD_ID",
                                                                       NODE_FACTORY.numberNode(4.9),
                caseFieldDefinition);
        assertEquals(1, numberNodeBelowMin.size(), "4.9 should not be with in range of between 5 and 10");
        assertEquals("Should be more than or equal to 5", numberNodeBelowMin.get(0).getErrorMessage());

        List<ValidationResult> textNodeAboveMin = validator.validate("TEST_FIELD_ID",
                                                                     NODE_FACTORY.textNode("10.1"),
                caseFieldDefinition);
        assertEquals(1, textNodeAboveMin.size(), "10.1 should not be with in range of between 5 and 10");
        assertEquals("Should be less than or equal to 10", textNodeAboveMin.get(0).getErrorMessage());

        List<ValidationResult> numberNodeAboveMin = validator.validate("TEST_FIELD_ID",
                                                                       NODE_FACTORY.numberNode(10.1),
                caseFieldDefinition);
        assertEquals(1,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(10.1), caseFieldDefinition).size(),
                     "10.1 should not be with in range of between 5 and 10");
        assertEquals("Should be less than or equal to 10", numberNodeAboveMin.get(0).getErrorMessage());
    }

    @Test
    void valueWithSameMaxMin() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(0)
                                               .withMax(0)
                                               .build();

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("0"), caseFieldDefinition).size(),
                     "0 should be with in range of between 0 and 0");

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(0), caseFieldDefinition).size(),
                     "0 should be with in range of between 0 and 0");

        assertEquals(1,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("-1"), caseFieldDefinition).size(),
                     "-1 should not be with in range of between 0 and 0");

        assertEquals(1,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(-1), caseFieldDefinition).size(),
                     "-1 should not be with in range of between 0 and 0");

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("0.0000000000"), caseFieldDefinition).size(),
                     "0.0000000000 should be with in range of between 0 and 0");

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(0.0000000000), caseFieldDefinition).size(),
                     "0.0000000000 should be with in range of between 0 and 0");
    }

    @Test
    void valueWithSameDecimalMaxMin() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(0.0f)
                                               .withMax(0.0f)
                                               .build();

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("0"), caseFieldDefinition).size(),
                     "0 should be with in range of between 0.00 and 0.00");

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(0), caseFieldDefinition).size(),
                     "0 should be with in range of between 0.00 and 0.00");
    }

    @Test
    void fieldTypeRegEx() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withRegExp("^\\d\\.\\d\\d$").build();

        assertEquals(0,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("8.20"), caseFieldDefinition).size(),
                     "regular expression check");

        List<ValidationResult> results = validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("8.2"), caseFieldDefinition);
        assertEquals(1, results.size(), "regular expression check");
        assertEquals(REGEX_GUIDANCE, results.get(0).getErrorMessage());
    }

    @Test
    void invalidBaseTypeRegEx() {
        when(numberBaseType.getRegularExpression()).thenReturn("\\d");

        final List<ValidationResult> result = validator.validate("TEST_FIELD_ID",
                                                                 NODE_FACTORY.numberNode(12), caseFieldDefinition);
        assertEquals(1, result.size(), "RegEx validation failed");
        assertEquals("'12' failed number Type Regex check: \\d", result.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", result.get(0).getFieldId());
    }

    @Test
    void incorrectFormat() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(5)
                                               .withMax(10)
                                               .build();

        assertEquals(1,
                     validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("10.1xxxx"), caseFieldDefinition).size(),
                     "Did not catch invalid 10.1xxxx");
    }

    @Test
    void nullValue() {
        assertEquals(0, validator.validate("TEST_FIELD_ID", null, null).size(), "Did not catch NULL");
    }

    @Test
    void getType() {
        assertEquals(validator.getType(), BaseType.get("NUMBER"), "Type is incorrect");
    }
}
