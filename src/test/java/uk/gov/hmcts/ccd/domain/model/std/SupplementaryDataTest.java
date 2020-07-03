package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.config.JacksonUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SupplementaryDataTest {

    @Test
    public void testConstruction() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonRequest = "{\n"
            + "\t\"$set\": {\n"
            + "\t\t\"orgs_assigned_users\": {\n"
            + "\t\t\"organisationA\": 32\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}";
        Map<String, JsonNode> value = JacksonUtils.convertValue(mapper.readTree(jsonRequest));
        SupplementaryData supplementaryData = new SupplementaryData(value.get("$set"), new HashSet<>());
        assertNotNull(supplementaryData);
        assertNotNull(supplementaryData.getResponse());
    }


}
