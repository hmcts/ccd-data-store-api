package uk.gov.hmcts.ccd.domain.model.aggregated.lite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CaseEventLite Test")
class CaseEventLiteTest {

    private CaseEventLite caseEventLite;

    @BeforeEach
    void setUp() {
        caseEventLite = new CaseEventLite();
    }

    @Test
    @DisplayName("Should create CaseEventLite with default values")
    void shouldCreateCaseEventLiteWithDefaultValues() {
        assertNotNull(caseEventLite);
        assertNull(caseEventLite.getId());
        assertNull(caseEventLite.getName());
        assertNull(caseEventLite.getDescription());
        assertNotNull(caseEventLite.getPreStates());
        assertTrue(caseEventLite.getPreStates().isEmpty());
        assertNull(caseEventLite.getAccessControlLists());
    }

    @Test
    @DisplayName("Should set and get id")
    void shouldSetAndGetId() {
        String id = "EVENT_1";
        caseEventLite.setId(id);
        assertEquals(id, caseEventLite.getId());
    }

    @Test
    @DisplayName("Should set and get name")
    void shouldSetAndGetName() {
        String name = "Test Event";
        caseEventLite.setName(name);
        assertEquals(name, caseEventLite.getName());
    }

    @Test
    @DisplayName("Should set and get description")
    void shouldSetAndGetDescription() {
        String description = "Test Event Description";
        caseEventLite.setDescription(description);
        assertEquals(description, caseEventLite.getDescription());
    }

    @Test
    @DisplayName("Should set and get preStates")
    void shouldSetAndGetPreStates() {
        List<String> preStates = Arrays.asList("State1", "State2", "State3");
        caseEventLite.setPreStates(preStates);
        assertEquals(preStates, caseEventLite.getPreStates());
        assertEquals(3, caseEventLite.getPreStates().size());
    }

    @Test
    @DisplayName("Should handle null preStates")
    void shouldHandleNullPreStates() {
        caseEventLite.setPreStates(null);
        assertNull(caseEventLite.getPreStates());
    }

    @Test
    @DisplayName("Should set and get accessControlLists")
    void shouldSetAndGetAccessControlLists() {
        final List<AccessControlList> acls = new ArrayList<>();
        AccessControlList acl1 = new AccessControlList();
        acl1.setAccessProfile("profile1");
        acl1.setCreate(true);
        acl1.setRead(true);
        acls.add(acl1);

        AccessControlList acl2 = new AccessControlList();
        acl2.setAccessProfile("profile2");
        acl2.setUpdate(true);
        acl2.setDelete(true);
        acls.add(acl2);

        caseEventLite.setAccessControlLists(acls);
        assertEquals(acls, caseEventLite.getAccessControlLists());
        assertEquals(2, caseEventLite.getAccessControlLists().size());
    }

    @Test
    @DisplayName("Should handle null accessControlLists")
    void shouldHandleNullAccessControlLists() {
        caseEventLite.setAccessControlLists(null);
        assertNull(caseEventLite.getAccessControlLists());
    }

    @Test
    @DisplayName("Should create copy of CaseEventLite")
    void shouldCreateCopyOfCaseEventLite() {
        caseEventLite.setId("EVENT_1");
        caseEventLite.setName("Test Event");
        caseEventLite.setDescription("Test Description");
        caseEventLite.setPreStates(Arrays.asList("State1", "State2"));

        List<AccessControlList> acls = new ArrayList<>();
        AccessControlList acl = new AccessControlList();
        acl.setAccessProfile("profile1");
        acl.setCreate(true);
        acls.add(acl);
        caseEventLite.setAccessControlLists(acls);

        CaseEventLite copy = caseEventLite.createCopy();

        assertNotNull(copy);
        assertNotSame(caseEventLite, copy);
        assertEquals(caseEventLite.getId(), copy.getId());
        assertEquals(caseEventLite.getName(), copy.getName());
        assertEquals(caseEventLite.getDescription(), copy.getDescription());
        assertEquals(caseEventLite.getPreStates(), copy.getPreStates());
        assertNotNull(copy.getAccessControlLists());
        assertEquals(caseEventLite.getAccessControlLists().size(), copy.getAccessControlLists().size());
    }

    @Test
    @DisplayName("Should create copy with null preStates")
    void shouldCreateCopyWithNullPreStates() {
        caseEventLite.setId("EVENT_1");
        caseEventLite.setName("Test Event");
        caseEventLite.setPreStates(null);

        CaseEventLite copy = caseEventLite.createCopy();

        assertNotNull(copy);
        assertNull(copy.getPreStates());
    }

    @Test
    @DisplayName("Should create copy with null accessControlLists")
    void shouldCreateCopyWithNullAccessControlLists() {
        caseEventLite.setId("EVENT_1");
        caseEventLite.setName("Test Event");
        caseEventLite.setAccessControlLists(null);

        CaseEventLite copy = caseEventLite.createCopy();

        assertNotNull(copy);
        assertNull(copy.getAccessControlLists());
    }

    @Test
    @DisplayName("Should create copy with empty preStates")
    void shouldCreateCopyWithEmptyPreStates() {
        caseEventLite.setId("EVENT_1");
        caseEventLite.setPreStates(new ArrayList<>());

        CaseEventLite copy = caseEventLite.createCopy();

        assertNotNull(copy);
        assertNotNull(copy.getPreStates());
        assertTrue(copy.getPreStates().isEmpty());
    }

    @Test
    @DisplayName("Should create independent copy - modifying copy should not affect original")
    void shouldCreateIndependentCopy() {
        caseEventLite.setId("EVENT_1");
        caseEventLite.setName("Original Name");
        List<String> preStates = new ArrayList<>(Arrays.asList("State1"));
        caseEventLite.setPreStates(preStates);

        CaseEventLite copy = caseEventLite.createCopy();
        copy.setName("Modified Name");
        copy.getPreStates().add("State2");

        assertEquals("Original Name", caseEventLite.getName());
        assertEquals(1, caseEventLite.getPreStates().size());
        assertEquals("Modified Name", copy.getName());
        assertEquals(2, copy.getPreStates().size());
    }
}
