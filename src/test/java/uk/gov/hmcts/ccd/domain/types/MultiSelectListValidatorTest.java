package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

import static java.math.BigDecimal.ONE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@DisplayName("MultiSelectListValidator")
class MultiSelectListValidatorTest {

    private static final JsonNodeFactory NODE_FACTORY = new JsonNodeFactory(Boolean.FALSE);
    private static final String FIELD_ID = "MultiSelectList1";
    private static final String OPTION_1 = "Option1";
    private static final String OPTION_2 = "Option2";
    private static final String OPTION_3 = "Option3";
    private static final String OPTION_UNKNOWN = "OptionUnknown";

    @Mock
    private BaseType multiSelectBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private CaseFieldDefinition caseFieldDefinition;

    private MultiSelectListValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(multiSelectBaseType.getType()).thenReturn(MultiSelectListValidator.TYPE_ID);
        BaseType.register(multiSelectBaseType);

        validator = new MultiSelectListValidator();

        caseFieldDefinition = caseField().build();
    }

    @Test
    void validate_shouldBeValidWhenNull() {
        final List<ValidationResult> results = validator.validate(FIELD_ID, null, caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldBeValidWhenNullNode() {
        final List<ValidationResult> results = validator.validate(FIELD_ID, NODE_FACTORY.nullNode(), caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldBeValidWhenArrayOfValidValues() {
        final ArrayNode values = NODE_FACTORY.arrayNode()
                                             .add(OPTION_1)
                                             .add(OPTION_2);

        final List<ValidationResult> results = validator.validate(FIELD_ID, values, caseFieldDefinition);

        assertThat(results, is(emptyCollectionOf(ValidationResult.class)));
    }

    @Test
    void validate_shouldNOTBeValidWhenValueIsNotAnArray() {
        final JsonNode value = NODE_FACTORY.textNode("Nayab was here, 24/07/2017");

        final List<ValidationResult> results = validator.validate(FIELD_ID, value, caseFieldDefinition);

        assertThat(results, hasSize(1));
    }

    @Test
    void validate_shouldNOTBeValidWhenContainsUnknownValue() {
        final ArrayNode values = NODE_FACTORY.arrayNode()
                                             .add(OPTION_1)
                                             .add(OPTION_UNKNOWN);

        final List<ValidationResult> results = validator.validate(FIELD_ID, values, caseFieldDefinition);

        assertThat(results, hasSize(1));
    }

    @Test
    void validate_shouldNOTBeValidWhenContainsDuplicateValues() {
        final ArrayNode values = NODE_FACTORY.arrayNode()
                                             .add(OPTION_1)
                                             .add(OPTION_1);

        final List<ValidationResult> results = validator.validate(FIELD_ID, values, caseFieldDefinition);

        assertThat(results, hasSize(1));
    }

    @Test
    void validate_shouldNOTBeValidWhenBelowMin() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(2).build();

        final ArrayNode values = NODE_FACTORY.arrayNode()
                                             .add(OPTION_1);

        final List<ValidationResult> results = validator.validate(FIELD_ID, values, caseFieldDefinition);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getErrorMessage(), equalTo("Select at least 2 options"));
    }

    @Test
    void validate_shouldNOTBeValidWhenAboveMax() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withMax(ONE).build();

        final ArrayNode values = NODE_FACTORY.arrayNode()
                                             .add(OPTION_1)
                                             .add(OPTION_2);

        final List<ValidationResult> results = validator.validate(FIELD_ID, values, caseFieldDefinition);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getErrorMessage(), equalTo("Cannot select more than 1 option"));
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(MultiSelectListValidator.TYPE_ID)
                                             .withFixedListItem(OPTION_1)
                                             .withFixedListItem(OPTION_2)
                                             .withFixedListItem(OPTION_3);
    }
}
