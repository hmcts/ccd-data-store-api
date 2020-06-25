package uk.gov.hmcts.ccd.data;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplementaryDataProcessorTest {

    @BeforeEach
    void setUp() {
    }

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
        childMap.put("childchild", childChildMap);

        Map<String, Object> leafNodes = new SupplementaryDataProcessor().accessLeafNodes(rootMap);

        assertTrue(leafNodes.containsKey("test2"));
        assertTrue(leafNodes.containsKey("test1"));
        assertTrue(leafNodes.containsKey("child1,testc1"));
        assertTrue(leafNodes.containsKey("child1,testc2"));
        assertTrue(leafNodes.containsKey("child1,childchild,testcc1"));
        assertTrue(leafNodes.containsKey("child1,childchild,testcc2"));
    }

    @Test
    void shouldReturnEmptyWhenRequestDataEmpty() {
        Map<String, Object> rootMap = new HashMap<>();

        Map<String, Object> leafNodes = new SupplementaryDataProcessor().accessLeafNodes(rootMap);
        assertTrue(leafNodes.size() == 0);
    }

    @Test
    void shouldReturnEmptyWhenRequestDataNull() {
        Map<String, Object> leafNodes = new SupplementaryDataProcessor().accessLeafNodes(null);
        assertTrue(leafNodes.size() == 0);
    }
}
