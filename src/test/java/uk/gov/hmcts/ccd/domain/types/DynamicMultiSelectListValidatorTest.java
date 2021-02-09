package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

    private DynamicMultiSelectListValidator validator;
    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(fixedListBaseType.getType()).thenReturn(DynamicListValidator.TYPE_ID);
        when(fixedListBaseType.getRegularExpression()).thenReturn(null);
        BaseType.register(fixedListBaseType);

        validator = new DynamicMultiSelectListValidator();

        caseFieldDefinition = caseField().build();
    }

    @Test
    public void validValueMultipleSelection() throws Exception {
        JsonNode dataValue = new ObjectMapper().readTree("{\n"
            + "\t\"value\": [{\n"
            + "\t\t\"code\": \"MONDAYFIRSTOFMAY\",\n"
            + "\t\t\"label\": \"Monday, May 1st\"\n"
            + "\t}, {\n"
            + "\n"
            + "\t\t\"code\": \"THURSDAYFOURTHOFMAY\",\n"
            + "\t\t\"label\": \"Thursday, May 4th\"\n"
            + "\t}],\n"
            + "\t\"list_items\": [{\n"
            + "\t\t\"code\": \"MONDAYFIRSTOFMAY\",\n"
            + "\t\t\"label\": \"Monday, May 1st\"\n"
            + "\t}, {\n"
            + "\t\t\"code\": \"TUESDAYSECONDOFMAY\",\n"
            + "\t\t\"label\": \"Tuesday, May 2nd\"\n"
            + "\t}, {\n"
            + "\t\t\"code\": \"WEDNESDAYTHIRDOFMAY\",\n"
            + "\t\t\"label\": \"Wednesday, May 3rd\"\n"
            + "\t}, {\n"
            + "\t\t\"code\": \"THURSDAYFOURTHOFMAY\",\n"
            + "\t\t\"label\": \"Thursday, May 4th\"\n"
            + "\t}]\n"
            + "}");

        final List<ValidationResult> result01 = validator.validate(TEST_FIELD_ID,
            dataValue,
            caseFieldDefinition);
        assertEquals(0, result01.size());
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength") // don't want to break long regex expressions
    public void invalidValue() throws Exception {
        JsonNode dataValue = new ObjectMapper().readTree("{\n" + "          \"default\": {\n"
            + "            \"code\": \"FixedList1\",\n"
            + "            \"label\": \"Fixed List 1\"\n"
            + "          },\n"
            + "          \"list_items\": [{\n"
            + "          \"code\": \"FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1"
            + "            FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1FixedList1FixedList\",\n"
            + "            \"label\": \"Fixed List 1\"\n"
            + "          }, {\n"
            + "            \"code\": \"FixedList2\",\n"
            + "            \"label\": \"Fixed List 2\"\n"
            + "          }, {\n"
            + "            \"code\": \"FixedList3\",\n"
            + "            \"label\": \"Fixed List 3\"\n"
            + "          }, {\n"
            + "            \"code\": \"FixedList4\",\n"
            + "            \"label\": \"Fixed List 4\"\n"
            + "          }, {\n"
            + "            \"code\": \"FixedList5\",\n"
            + "            \"label\": \"Fixed List 5\"\n"
            + "          }, {\n"
            + "            \"code\": \"FixedList6\",\n"
            + "            \"label\": \"Fixed List 6\"\n"
            + "          }, {\n"
            + "            \"code\": \"FixedList7\",\n"
            + "            \"label\": \"Fixed List 7\"\n"
            + "          }\n"
            + "          ]\n"
            + "        }");


        final List<ValidationResult> result01 = validator.validate(TEST_FIELD_ID,
            dataValue,
            caseFieldDefinition);
        assertEquals(1, result01.size(), result01.toString());
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
