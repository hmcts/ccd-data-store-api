package uk.gov.hmcts.ccd.domain.model.std;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataOperation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplementaryDataUpdateRequestTest {

    @Test
    void accessLeafNodes() {
        Map<String, Object> rootMap = new HashMap<>();
        rootMap.put("test1", "Test Value");
        rootMap.put("test2", "Test Value 2");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("testc1", "Test Value c1");
        childMap.put("testc2", "Test Value c2");

        rootMap.put("child1", childMap);

        Map<String, Object> childChildMap = new HashMap<>();
        childChildMap.put("testcc1", "Test Value cc1");
        childChildMap.put("testcc2", "Test Value cc2");
        childChildMap.put("testcc3", true);
        childChildMap.put("testcc4", 44);
        childMap.put("childchild", childChildMap);

        Map<String, Map<String, Object>> requestMap = new HashMap<>();
        requestMap.put(SupplementaryDataOperation.SET.getOperationName(), rootMap);
        SupplementaryDataUpdateRequest updateRequest = new SupplementaryDataUpdateRequest(requestMap);

        Map<String, Object> leafNodes = updateRequest.getUpdateOperationProperties(SupplementaryDataOperation.SET);

        assertTrue(leafNodes.containsKey("test2"));
        assertTrue(leafNodes.containsKey("test1"));
        assertTrue(leafNodes.containsKey("child1,testc1"));
        assertTrue(leafNodes.containsKey("child1,testc2"));
        assertTrue(leafNodes.containsKey("child1,childchild,testcc1"));
        assertEquals("Test Value cc1", leafNodes.get("child1,childchild,testcc1"));
        assertTrue(leafNodes.containsKey("child1,childchild,testcc2"));
        assertEquals("Test Value cc2", leafNodes.get("child1,childchild,testcc2"));
        assertTrue(leafNodes.containsKey("child1,childchild,testcc3"));
        assertEquals(true, leafNodes.get("child1,childchild,testcc3"));
        assertTrue(leafNodes.containsKey("child1,childchild,testcc4"));
        assertEquals(44, leafNodes.get("child1,childchild,testcc4"));
    }

    @Test
    void shouldReturnEmptyWhenRequestDataEmpty() {
        Map<String, Map<String, Object>> rootMap = new HashMap<>();

        SupplementaryDataUpdateRequest updateRequest = new SupplementaryDataUpdateRequest(rootMap);
        Map<String, Object> leafNodes = updateRequest.getUpdateOperationProperties(SupplementaryDataOperation.SET);
        assertTrue(leafNodes.size() == 0);
    }
}
