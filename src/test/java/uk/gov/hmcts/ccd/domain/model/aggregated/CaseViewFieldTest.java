package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class CaseViewFieldTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CASE_FIELD_ID = "CaseFieldID";
    private static final String RELATIVE_CASE_FIELD_SUBFIELD_CODE = "Name";
    private static final String CASE_FIELD_SUBFIELD_CODE = CASE_FIELD_ID + "." + RELATIVE_CASE_FIELD_SUBFIELD_CODE;
    private static final String CASE_FIELD_SUBFIELD_CODE_PROPERTY = "caseFieldSubfieldCode";

    @Test
    void shouldSerializeCaseFieldSubfieldCodeWhenPresent() throws Exception {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId(CASE_FIELD_ID);
        caseViewField.setCaseFieldSubfieldCode(CASE_FIELD_SUBFIELD_CODE);

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(caseViewField));

        assertThat(json.get("id").asText(), is(CASE_FIELD_ID));
        assertThat(json.get(CASE_FIELD_SUBFIELD_CODE_PROPERTY).asText(), is(CASE_FIELD_SUBFIELD_CODE));
    }

    @Test
    void shouldOmitCaseFieldSubfieldCodeWhenAbsent() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(new CaseViewField()));

        assertThat(json.has(CASE_FIELD_SUBFIELD_CODE_PROPERTY), is(false));
    }

    @Test
    void shouldOmitCaseFieldSubfieldCodeWhenBlank() throws Exception {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setCaseFieldSubfieldCode("");

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(caseViewField));

        assertThat(json.has(CASE_FIELD_SUBFIELD_CODE_PROPERTY), is(false));
    }

    @Test
    void shouldPrefixCaseFieldIdWhenCreatingFromRelativeCaseFieldSubfieldCode() {
        CaseViewField caseViewField = CaseViewField.createFrom(
            caseTypeTabField(CASE_FIELD_ID, RELATIVE_CASE_FIELD_SUBFIELD_CODE),
            null
        );

        assertThat(caseViewField.getCaseFieldSubfieldCode(), is(CASE_FIELD_SUBFIELD_CODE));
    }

    @Test
    void shouldKeepAbsoluteCaseFieldSubfieldCodeWhenCreatingFromCaseTypeTabField() {
        CaseViewField caseViewField = CaseViewField.createFrom(
            caseTypeTabField(CASE_FIELD_ID, CASE_FIELD_SUBFIELD_CODE),
            null
        );

        assertThat(caseViewField.getCaseFieldSubfieldCode(), is(CASE_FIELD_SUBFIELD_CODE));
    }

    private CaseTypeTabField caseTypeTabField(String caseFieldId, String caseFieldSubfieldCode) {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setId(caseFieldId);
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);

        CaseTypeTabField caseTypeTabField = new CaseTypeTabField();
        caseTypeTabField.setCaseFieldDefinition(caseFieldDefinition);
        caseTypeTabField.setCaseFieldSubfieldCode(caseFieldSubfieldCode);

        return caseTypeTabField;
    }
}
