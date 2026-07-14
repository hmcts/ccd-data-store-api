package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class CaseTypeTabFieldTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserializeListElementCode() throws Exception {
        CaseTypeTabField tabField = new CaseTypeTabField();
        tabField.setListElementCode("FamilyAddress.Country");

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(tabField));
        CaseTypeTabField deserialized = MAPPER.readValue(
            "{\"listElementCode\":\"FamilyAddress.Country\"}",
            CaseTypeTabField.class
        );

        assertThat(json.get("listElementCode").asText(), is("FamilyAddress.Country"));
        assertThat(deserialized.getListElementCode(), is("FamilyAddress.Country"));
    }
}
