package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class CaseViewFieldTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldSerializeCaseFieldSubfieldCodeWhenPresent() throws Exception {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId("CaseFieldID");
        caseViewField.setCaseFieldSubfieldCode("CaseFieldID.Name");

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(caseViewField));

        assertThat(json.get("id").asText(), is("CaseFieldID"));
        assertThat(json.get("caseFieldSubfieldCode").asText(), is("CaseFieldID.Name"));
    }

    @Test
    void shouldOmitCaseFieldSubfieldCodeWhenAbsent() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(new CaseViewField()));

        assertThat(json.has("caseFieldSubfieldCode"), is(false));
    }

    @Test
    void shouldOmitCaseFieldSubfieldCodeWhenBlank() throws Exception {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setCaseFieldSubfieldCode("");

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(caseViewField));

        assertThat(json.has("caseFieldSubfieldCode"), is(false));
    }
}
