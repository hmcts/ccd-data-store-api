package uk.gov.hmcts.ccd.domain.types;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FixedListItemDefinition;

@DisplayName("FixedRadioListValidator")
class FixedRadioListValidatorTest {
    public static final String TEST_FIELD_ID = "TEST_FIELD_ID";
    private static final String FIELD_ID = TEST_FIELD_ID;
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    public static final String FIXED_RADIO_LIST = "FixedRadioList";

    @Mock
    private BaseType fixedListBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private FixedRadioListValidator validator;
    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(fixedListBaseType.getType()).thenReturn(FixedRadioListValidator.TYPE_ID);
        when(fixedListBaseType.getRegularExpression()).thenReturn(null);
        BaseType.register(fixedListBaseType);

        validator = new FixedRadioListValidator();

        caseFieldDefinition = caseField().build();
    }

    @Test
    void validValue() {
        final List<ValidationResult> result01 = validator.validate(TEST_FIELD_ID,
            NODE_FACTORY.textNode("AAAAAA"),
                caseFieldDefinition);
        assertEquals(0, result01.size());
    }

    @Test
    void invalidValue() {
        final List<ValidationResult> result01 = validator.validate(TEST_FIELD_ID,
            NODE_FACTORY.textNode("DDDD"),
                caseFieldDefinition);
        assertEquals(1, result01.size(), result01.toString());
    }

    @Test
    void nullValue() {
        assertEquals(0, validator.validate(TEST_FIELD_ID, null, null).size(), "Did not catch NULL");
    }

    @Test
    void getType() {
        assertEquals(validator.getType(), BaseType.get(FIXED_RADIO_LIST), "Type is incorrect");
    }

    private FieldTypeDefinition.FieldTypeDefinitionBuilder defaultFieldDefinition() {
        return FieldTypeDefinition.builder()
            .fixedListItemDefinitions(List.of(
                new FixedListItemDefinition("AAAAAA", null, null),
                new FixedListItemDefinition("BBBBBB", null, null),
                new FixedListItemDefinition("CCCCCC", null, null)
            ))
            .type(FIXED_RADIO_LIST);
    }

    private CaseFieldDefinition.CaseFieldDefinitionBuilder caseField() {
        return CaseFieldDefinition.builder()
            .id(FIELD_ID)
            .fieldTypeDefinition(defaultFieldDefinition()
                .build());
    }
}
