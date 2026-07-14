package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class CaseViewFieldTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldSerializeListElementCodeWhenPresent() throws Exception {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setListElementCode("Name");

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(caseViewField));

        assertThat(json.get("listElementCode").asText(), is("Name"));
    }

    @Test
    void shouldOmitListElementCodeWhenAbsent() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(new CaseViewField()));

        assertThat(json.has("listElementCode"), is(false));
    }

    @Test
    void shouldOmitListElementCodeWhenBlank() throws Exception {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setListElementCode("");

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(caseViewField));

        assertThat(json.has("listElementCode"), is(false));
    }
}
