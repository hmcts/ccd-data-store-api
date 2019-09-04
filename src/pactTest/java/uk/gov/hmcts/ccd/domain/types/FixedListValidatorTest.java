package uk.gov.hmcts.ccd.domain.types;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.types.BaseTypeValidator.REGEX_GUIDANCE;

@DisplayName("FixedListValidator")
class FixedListValidatorTest {
    private static final String FIELD_ID = "TEST_FIELD_ID";
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

    @Mock
    private BaseType fixedListBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private FixedListValidator validator;
    private CaseField caseField;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(fixedListBaseType.getType()).thenReturn(FixedListValidator.TYPE_ID);
        when(fixedListBaseType.getRegularExpression()).thenReturn(null);
        BaseType.register(fixedListBaseType);

        validator = new FixedListValidator();

        caseField = caseField().build();
    }

    @Test
    void validValue() {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
                                                                   NODE_FACTORY.textNode("AAAAAA"),
                                                                   caseField);
        assertEquals(0, result01.size());
    }

    @Test
    void invalidValue() {
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID",
                                                                   NODE_FACTORY.textNode("DDDD"),
                                                                   caseField);
        assertEquals(1, result01.size(), result01.toString());
    }

    @Test
    void nullValue() {
        assertEquals(0, validator.validate("TEST_FIELD_ID", null, null).size(), "Did not catch NULL");
    }

    @Test
    void getType() {
        assertEquals(validator.getType(), BaseType.get("FixedList"), "Type is incorrect");
    }

    @Test
    void fieldTypeRegEx() {
        final CaseField caseFieldWithRegEx = caseField().withRegExp("AAAAAA").build();
        final List<ValidationResult> result01 = validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("AAAAAA"),
                                                                   caseFieldWithRegEx);
        assertEquals(0, result01.size());

        final List<ValidationResult> result02 = validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("BBBBBB"),
                                                                   caseFieldWithRegEx);
        assertEquals(1, result02.size(), "BBBBBB failed regular expression check");
        assertEquals(REGEX_GUIDANCE, result02.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", result02.get(0).getFieldId());
    }

    @Test
    void baseTypeRegEx() {
        when(fixedListBaseType.getRegularExpression()).thenReturn("InvalidRegEx");
        final List<ValidationResult> result = validator.validate("TEST_FIELD_ID",
                                                                 NODE_FACTORY.textNode("AA"), caseField);
        assertEquals(1, result.size(), "RegEx validation failed");
        assertEquals("'AA' failed FixedList Type Regex check: InvalidRegEx", result.get(0).getErrorMessage());
        assertEquals("TEST_FIELD_ID", result.get(0).getFieldId());
    }

    private CaseFieldBuilder caseField() {
        return new CaseFieldBuilder(FIELD_ID).withType(FixedListValidator.TYPE_ID)
                                             .withFixedListItem("AAAAAA")
                                             .withFixedListItem("BBBBBB")
                                             .withFixedListItem("CCCCCC");
    }
}
