package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.config.JacksonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SupplementaryDataTest {

    private static final String VALID_KEY = "orgs_assigned_users.organisationA";
    private static final String INVALID_KEY = "orgs_assigned_users.test";

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
        SupplementaryData supplementaryData = new SupplementaryData(
            "test", value.get("$set"), Collections.singleton(VALID_KEY));

        assertNotNull(supplementaryData);
        assertNotNull(supplementaryData.getResponse());
        assertEquals(1, supplementaryData.getResponse().size());
        assertEquals(32, supplementaryData.getResponse().get(VALID_KEY));
    }

    @Test
    public void testConstructionFailure() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonRequest = "{\n"
            + "\t\"$inc\": {\n"
            + "\t\t\"orgs_assigned_users\": {\n"
            + "\t\t\"organisationA\": 32\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}";

        Map<String, JsonNode> value = JacksonUtils.convertValue(mapper.readTree(jsonRequest));
        SupplementaryData supplementaryData = new SupplementaryData(
            "test", value.get("$inc"), Collections.singleton(INVALID_KEY));

        assertNotNull(supplementaryData);
        assertNotNull(supplementaryData.getResponse());
        assertEquals(0, supplementaryData.getResponse().size());
    }

}
