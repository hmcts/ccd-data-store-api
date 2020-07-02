package uk.gov.hmcts.ccd.domain.model.std;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataOperation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplementaryDataUpdateRequestTest {

    @Test
    void accessUpdateOperationProperties() {
        Map<String, Map<String, Object>> rootMap = new HashMap<>();

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("testc1.testchild1", "Test Value c1");
        childMap.put("testc1.testchild2", "Test Value c2");
        rootMap.put("$set", childMap);

        SupplementaryDataUpdateRequest updateRequest = new SupplementaryDataUpdateRequest(rootMap);

        Map<String, Object> leafNodes = updateRequest.getUpdateOperationProperties(SupplementaryDataOperation.SET);

        assertTrue(leafNodes.containsKey("testc1,testchild1"));
        assertEquals("Test Value c1", leafNodes.get("testc1,testchild1"));
        assertTrue(leafNodes.containsKey("testc1,testchild2"));
        assertEquals("Test Value c2", leafNodes.get("testc1,testchild2"));

        Map<String, Object> convertedData = updateRequest.convertRequestData(SupplementaryDataOperation.SET);
        assertTrue(convertedData.containsKey("testc1"));
        Map<String, Object> childMapResponse = (Map<String, Object>) convertedData.get("testc1");
        assertTrue(childMapResponse.containsKey("testchild1"));
        assertEquals("Test Value c2", childMapResponse.get("testchild2"));
        assertEquals("Test Value c1", childMapResponse.get("testchild1"));
    }

    @Test
    void shouldReturnEmptyWhenRequestDataEmpty() {
        Map<String, Map<String, Object>> rootMap = new HashMap<>();

        SupplementaryDataUpdateRequest updateRequest = new SupplementaryDataUpdateRequest(rootMap);
        Map<String, Object> leafNodes = updateRequest.getUpdateOperationProperties(SupplementaryDataOperation.SET);
        assertTrue(leafNodes.size() == 0);
    }
}
