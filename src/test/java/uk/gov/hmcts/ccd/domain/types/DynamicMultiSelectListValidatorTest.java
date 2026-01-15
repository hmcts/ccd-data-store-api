package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.test.CaseFieldDefinitionBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.types.DynamicMultiSelectListValidator.TYPE_ID;

class DynamicMultiSelectListValidatorTest {

    public static final String TEST_FIELD_ID = "TEST_FIELD_ID";
    private static final String FIELD_ID = TEST_FIELD_ID;

    @Mock
    private BaseType fixedListBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    @Mock
    private ApplicationParams applicationParams;

    private DynamicMultiSelectListValidator validator;
    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(fixedListBaseType.getType()).thenReturn(DynamicListValidator.TYPE_ID);
        when(fixedListBaseType.getRegularExpression()).thenReturn(null);
        BaseType.register(fixedListBaseType);

        when(applicationParams.getValidationDynamicListCodeMaxLength()).thenReturn(150);
        when(applicationParams.getValidationDynamicListValueMaxLength()).thenReturn(500);

        validator = new DynamicMultiSelectListValidator(applicationParams);

        caseFieldDefinition = caseField().build();
    }

    @Test
    public void validValueMultipleSelection() throws Exception {
        JsonNode dataValue = new ObjectMapper().readTree("""
            {
            \t"value": [{
            \t\t"code": "MONDAYFIRSTOFMAY",
            \t\t"label": "Monday, May 1st"
            \t}, {

            \t\t"code": "THURSDAYFOURTHOFMAY",
            \t\t"label": "Thursday, May 4th"
            \t}],
            \t"list_items": [{
            \t\t"code": "MONDAYFIRSTOFMAY",
            \t\t"label": "Monday, May 1st"
            \t}, {
            \t\t"code": "TUESDAYSECONDOFMAY",
            \t\t"label": "Tuesday, May 2nd"
            \t}, {
            \t\t"code": "WEDNESDAYTHIRDOFMAY",
            \t\t"label": "Wednesday, May 3rd"
            \t}, {
            \t\t"code": "THURSDAYFOURTHOFMAY",
            \t\t"label": "Thursday, May 4th"
            \t}]
            }""");

        final List<ValidationResult> result01 = validator.validate(TEST_FIELD_ID,
            dataValue,
            caseFieldDefinition);
        assertEquals(0, result01.size());
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength") // don't want to break long regex expressions
    public void invalidValue() throws Exception {
        JsonNode dataValue = new ObjectMapper().readTree("""
            {
                      "default": {
                        "code": "FixedList1",
                        "label": "Fixed List 1"
                      },
                      "list_items": [{
                      "code": "FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1\
                        FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1FixedList",
                        "label": "Fixed List 1"
                      }, {
                        "code": "FixedList2",
                        "label": "Fixed List 2"
                      }, {
                        "code": "FixedList3",
                        "label": "Fixed List 3"
                      }, {
                        "code": "FixedList4",
                        "label": "Fixed List 4"
                      }, {
                        "code": "FixedList5",
                        "label": "Fixed List 5"
                      }, {
                        "code": "FixedList6",
                        "label": "Fixed List 6"
                      }, {
                        "code": "FixedList7",
                        "label": "Fixed List 7"
                      }
                      ]
                    }""");


        final List<ValidationResult> result01 = validator.validate(TEST_FIELD_ID,
            dataValue,
            caseFieldDefinition);
        assertEquals(1, result01.size(), result01.toString());
    }

    @Test
    public void invalidCodeMaxLength() throws Exception {
        when(applicationParams.getValidationDynamicListCodeMaxLength()).thenReturn(2);
        when(applicationParams.getValidationDynamicListValueMaxLength()).thenReturn(500);
        validator = new DynamicMultiSelectListValidator(applicationParams);

        JsonNode dataValue = new ObjectMapper().readTree("""
            {
            \t"value": [{
            \t\t"code": "MONDAYFIRSTOFMAY",
            \t\t"label": "Monday, May 1st"
            \t}, {

            \t\t"code": "THURSDAYFOURTHOFMAY",
            \t\t"label": "Thursday, May 4th"
            \t}],
            \t"list_items": [{
            \t\t"code": "MONDAYFIRSTOFMAY",
            \t\t"label": "Monday, May 1st"
            \t}, {
            \t\t"code": "TUESDAYSECONDOFMAY",
            \t\t"label": "Tuesday, May 2nd"
            \t}, {
            \t\t"code": "WEDNESDAYTHIRDOFMAY",
            \t\t"label": "Wednesday, May 3rd"
            \t}, {
            \t\t"code": "THURSDAYFOURTHOFMAY",
            \t\t"label": "Thursday, May 4th"
            \t}]
            }""");

        final List<ValidationResult> result01 = validator.validate(TEST_FIELD_ID,
            dataValue,
            caseFieldDefinition);
        assertEquals(6, result01.size());
    }

    @Test
    public void invalidValueMaxLength() throws Exception {
        when(applicationParams.getValidationDynamicListCodeMaxLength()).thenReturn(150);
        when(applicationParams.getValidationDynamicListValueMaxLength()).thenReturn(5);
        validator = new DynamicMultiSelectListValidator(applicationParams);

        JsonNode dataValue = new ObjectMapper().readTree("""
            {
            \t"value": [{
            \t\t"code": "MONDAYFIRSTOFMAY",
            \t\t"label": "Monday, May 1st"
            \t}, {

            \t\t"code": "THURSDAYFOURTHOFMAY",
            \t\t"label": "Thursday, May 4th"
            \t}],
            \t"list_items": [{
            \t\t"code": "MONDAYFIRSTOFMAY",
            \t\t"label": "Monday, May 1st"
            \t}, {
            \t\t"code": "TUESDAYSECONDOFMAY",
            \t\t"label": "Tuesday, May 2nd"
            \t}, {
            \t\t"code": "WEDNESDAYTHIRDOFMAY",
            \t\t"label": "Wednesday, May 3rd"
            \t}, {
            \t\t"code": "THURSDAYFOURTHOFMAY",
            \t\t"label": "Thursday, May 4th"
            \t}]
            }""");

        final List<ValidationResult> result01 = validator.validate(TEST_FIELD_ID,
            dataValue,
            caseFieldDefinition);
        assertEquals(6, result01.size());
    }

    @Test
    public void nullValue() {
        assertEquals(0, validator.validate(TEST_FIELD_ID, null, null).size(), "Did not catch NULL");
    }

    @Test
    public void getType() {
        assertEquals(validator.getType(), BaseType.get(TYPE_ID), "Type is incorrect");
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(TYPE_ID)
            .withDynamicListItem("AAAAAA", "A Value")
            .withDynamicListItem("BBBBBB", "B Value")
            .withDynamicListItem("CCCCCC", "C Value");
    }
}
