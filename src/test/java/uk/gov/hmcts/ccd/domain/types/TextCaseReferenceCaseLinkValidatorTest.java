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
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.test.CaseFieldDefinitionBuilder;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

@DisplayName("TextCaseReferenceCaseLinkValidator")
class TextCaseReferenceCaseLinkValidatorTest {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";

    @Mock
    private BaseType textAreaBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    @Mock
    private CaseService caseService;

    private TextCaseReferenceCaseLinkValidator validator;
    private TextValidator textValidator;
    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(textAreaBaseType.getType()).thenReturn(TextAreaValidator.TYPE_ID);
        BaseType.register(textAreaBaseType);
        textValidator =  new TextValidator();
        validator = new TextCaseReferenceCaseLinkValidator(textValidator,caseService);

        caseFieldDefinition = caseField().build();
    }

    @Test
    void getType() {
        assertThat(validator.getPredefinedFieldId(), is("TextCaseReference"));
    }

    @Test
    @DisplayName("should pass test against regular expression")
    void textRegexPass() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withRegExp("(?:^[0-9]{16}$|^\\d{4}-\\d{4}-\\d{4}-\\d{4}$)").build();
        final JsonNode validValue = NODE_FACTORY.textNode("1596-1048-4059-0000");
        final List<ValidationResult> validResult = validator.validate(FIELD_ID, validValue, caseFieldDefinition);

        final JsonNode invalidValue = NODE_FACTORY.textNode("1596104840593131");
        final List<ValidationResult> invalidResult = validator.validate(FIELD_ID, invalidValue, caseFieldDefinition);

        assertAll(
            () -> assertThat("Expected input to be valid", validResult, hasSize(0)),
            () -> assertThat("Expected input NOT to be valid", invalidResult, hasSize(0))
        );
    }

    @Test
    @DisplayName("should fail test against regular expression")
    void textRegexFail() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withRegExp("(?:^[0-9]{16}$|^\\d{4}-\\d{4}-\\d{4}-\\d{4}$)").build();
        final JsonNode validValue = NODE_FACTORY.textNode("15xxxx00");
        final List<ValidationResult> validResult = validator.validate(FIELD_ID, validValue, caseFieldDefinition);

        final JsonNode invalidValue = NODE_FACTORY.textNode("15961077777774840593131");
        final List<ValidationResult> invalidResult = validator.validate(FIELD_ID, invalidValue, caseFieldDefinition);

        assertAll(
            () -> assertThat("Expected input to be valid", validResult, hasSize(1)),
            () -> assertThat("Expected input NOT to be valid", invalidResult, hasSize(1))
        );
    }


    @Test
    @DisplayName("should fail test against due to resource not found")
    void failDueToResourceNotFound() {
        final CaseFieldDefinition caseFieldDefinition = caseField().withRegExp("(?:^[0-9]{16}$|^\\d{4}-\\d{4}-\\d{4}-\\d{4}$)").build();
        final String caseReference = "1596-1048-4059-0000";
        final JsonNode validValue = NODE_FACTORY.textNode(caseReference);

        when(caseService.getCaseDetailsByCaseReference("1596104840590000")).thenThrow(
            new ResourceNotFoundException("No case exist with id=" + caseReference)
        );
        final List<ValidationResult> validResult = validator.validate(FIELD_ID, validValue, caseFieldDefinition);

        final JsonNode invalidValue = NODE_FACTORY.textNode("1596104840593131");
        final List<ValidationResult> invalidResult = validator.validate(FIELD_ID, invalidValue, caseFieldDefinition);

        assertAll(
            () -> assertThat("Expected input to be valid", validResult, hasSize(1)),
            () -> assertThat("Expected input NOT to be valid", invalidResult, hasSize(0))
        );
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(TextAreaValidator.TYPE_ID);
    }
}
