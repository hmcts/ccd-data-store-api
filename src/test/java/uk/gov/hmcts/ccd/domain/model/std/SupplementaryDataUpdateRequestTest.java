package uk.gov.hmcts.ccd.domain.model.std;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataOperation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
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

        Map<String, Object> leafNodes = updateRequest.getOperationProperties(SupplementaryDataOperation.SET);

        assertTrue(leafNodes.containsKey("testc1.testchild1"));
        assertEquals("Test Value c1", leafNodes.get("testc1.testchild1"));
        assertTrue(leafNodes.containsKey("testc1.testchild2"));
        assertEquals("Test Value c2", leafNodes.get("testc1.testchild2"));
    }

    @Test
    void testGetPropertiesNames() {
        Map<String, Map<String, Object>> rootMap = new HashMap<>();

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("testc1.testchild1", "Test Value c1");
        childMap.put("testc1.testchild2", "Test Value c2");
        rootMap.put("$set", childMap);

        Map<String, Object> childMap2 = new HashMap<>();
        childMap2.put("testc3", "Test Value c3");
        childMap2.put("testc4.testchild4", "Test Value c4");
        rootMap.put("$inc", childMap2);

        SupplementaryDataUpdateRequest updateRequest = new SupplementaryDataUpdateRequest(rootMap);

        Set<String> propertiesNames = updateRequest.getPropertiesNames();

        assertThat(propertiesNames, hasSize(4));
        assertThat(propertiesNames, containsInAnyOrder("testc1.testchild1", "testc1.testchild2", "testc3", "testc4.testchild4"));
    }

    @Test
    void testGetSupplementaryDataOperations() {
        Map<String, Map<String, Object>> rootMap = new HashMap<>();

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("testc1.testchild1", "Test Value c1");
        rootMap.put("$set", childMap);

        Map<String, Object> childMap2 = new HashMap<>();
        childMap2.put("testc3", "Test Value c3");
        rootMap.put("$inc", childMap2);

        SupplementaryDataUpdateRequest updateRequest = new SupplementaryDataUpdateRequest(rootMap);

        Set<String> propertiesNames = updateRequest.getOperations();

        assertThat(propertiesNames, hasSize(2));
        assertThat(propertiesNames, containsInAnyOrder("$set", "$inc"));
    }

    @Test
    void shouldReturnEmptyWhenRequestDataEmpty() {
        Map<String, Map<String, Object>> rootMap = new HashMap<>();

        SupplementaryDataUpdateRequest updateRequest = new SupplementaryDataUpdateRequest(rootMap);
        Map<String, Object> leafNodes = updateRequest.getOperationProperties(SupplementaryDataOperation.SET);
        assertTrue(leafNodes.size() == 0);
    }
}
