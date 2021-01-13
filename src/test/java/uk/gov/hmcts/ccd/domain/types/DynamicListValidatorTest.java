package uk.gov.hmcts.ccd.domain.types;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.test.CaseFieldDefinitionBuilder;

@DisplayName("DynamicListValidator")
class DynamicListValidatorTest {
    public static final String TEST_FIELD_ID = "TEST_FIELD_ID";
    private static final String FIELD_ID = TEST_FIELD_ID;
    public static final String DYNAMIC_LIST = "DynamicList";

    @Mock
    private BaseType fixedListBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private DynamicListValidator validator;
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

        validator = new DynamicListValidator();

        caseFieldDefinition = caseField().build();
    }

    @Test
    public void validValue() throws Exception {
        JsonNode dataValue = new ObjectMapper().readTree("{\n" + "          \"value\": {\n"
            + "            \"code\": \"FixedList1\",\n"
            + "            \"label\": \"Fixed List 1\"\n"
            + "          },\n"
            + "          \"list_items\": [{\n"
            + "            \"code\": \"FixedList1\",\n"
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
        assertEquals(validator.getType(), BaseType.get(DYNAMIC_LIST), "Type is incorrect");
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(DYNAMIC_LIST)
            .withDynamicListItem("AAAAAA", "A Value")
            .withDynamicListItem("BBBBBB", "B Value")
            .withDynamicListItem("CCCCCC", "C Value");
    }
}
