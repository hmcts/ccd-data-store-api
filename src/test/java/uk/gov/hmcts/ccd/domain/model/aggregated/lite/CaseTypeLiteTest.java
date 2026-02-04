package uk.gov.hmcts.ccd.domain.model.aggregated.lite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CaseTypeLite Test")
class CaseTypeLiteTest {

    private CaseTypeDefinition caseTypeDefinition;
    private Version version;

    @BeforeEach
    void setUp() {
        version = new Version();
        version.setNumber(1);

        JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId("TEST_JURISDICTION");
        jurisdictionDefinition.setName("Test Jurisdiction");

        caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId("TEST_CASE_TYPE");
        caseTypeDefinition.setName("Test Case Type");
        caseTypeDefinition.setDescription("Test Description");
        caseTypeDefinition.setVersion(version);
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
        caseTypeDefinition.setSecurityClassification(SecurityClassification.PUBLIC);
    }

    @Test
    @DisplayName("Should create CaseTypeLite from CaseTypeDefinition")
    void shouldCreateCaseTypeLiteFromCaseTypeDefinition() {
        CaseTypeLite caseTypeLite = new CaseTypeLite(caseTypeDefinition);

        assertNotNull(caseTypeLite);
        assertEquals(caseTypeDefinition.getId(), caseTypeLite.getId());
        assertEquals(caseTypeDefinition.getName(), caseTypeLite.getName());
        assertEquals(caseTypeDefinition.getDescription(), caseTypeLite.getDescription());
        assertEquals(caseTypeDefinition.getVersion(), caseTypeLite.getVersion());
        assertEquals(caseTypeDefinition.getSecurityClassification(), caseTypeLite.getSecurityClassification());
        assertNotNull(caseTypeLite.getEvents());
        assertNotNull(caseTypeLite.getStates());
    }

    @Test
    @DisplayName("Should create CaseTypeLite with events and states from CaseTypeDefinition")
    void shouldCreateCaseTypeLiteWithEventsAndStates() {
        // Create states
        CaseStateDefinition state1 = new CaseStateDefinition();
        state1.setId("STATE_1");
        state1.setName("State 1");
        state1.setDescription("State 1 Description");

        CaseStateDefinition state2 = new CaseStateDefinition();
        state2.setId("STATE_2");
        state2.setName("State 2");
        state2.setDescription("State 2 Description");

        caseTypeDefinition.getStates().add(state1);
        caseTypeDefinition.getStates().add(state2);

        // Create events
        CaseEventDefinition event1 = new CaseEventDefinition();
        event1.setId("EVENT_1");
        event1.setName("Event 1");
        event1.setDescription("Event 1 Description");
        event1.setPreStates(Arrays.asList("State1", "State2"));

        CaseEventDefinition event2 = new CaseEventDefinition();
        event2.setId("EVENT_2");
        event2.setName("Event 2");
        event2.setDescription("Event 2 Description");

        List<AccessControlList> acls = new ArrayList<>();
        AccessControlList acl = new AccessControlList();
        acl.setAccessProfile("profile1");
        acl.setCreate(true);
        acls.add(acl);
        event1.setAccessControlLists(acls);

        caseTypeDefinition.getEvents().add(event1);
        caseTypeDefinition.getEvents().add(event2);

        CaseTypeLite caseTypeLite = new CaseTypeLite(caseTypeDefinition);

        assertNotNull(caseTypeLite);
        assertEquals(2, caseTypeLite.getEvents().size());
        assertEquals(2, caseTypeLite.getStates().size());

        CaseEventLite eventLite1 = caseTypeLite.getEvents().get(0);
        assertEquals("EVENT_1", eventLite1.getId());
        assertEquals("Event 1", eventLite1.getName());
        assertEquals("Event 1 Description", eventLite1.getDescription());
        assertEquals(Arrays.asList("State1", "State2"), eventLite1.getPreStates());
        assertNotNull(eventLite1.getAccessControlLists());
        assertEquals(1, eventLite1.getAccessControlLists().size());

        CaseStateLite stateLite1 = caseTypeLite.getStates().get(0);
        assertEquals("STATE_1", stateLite1.getId());
        assertEquals("State 1", stateLite1.getName());
        assertEquals("State 1 Description", stateLite1.getDescription());
    }

    @Test
    @DisplayName("Should create CaseTypeLite with empty events and states when null")
    void shouldCreateCaseTypeLiteWithEmptyListsWhenNull() {
        caseTypeDefinition.setEvents(null);
        caseTypeDefinition.setStates(null);

        CaseTypeLite caseTypeLite = new CaseTypeLite(caseTypeDefinition);

        assertNotNull(caseTypeLite);
        assertNotNull(caseTypeLite.getEvents());
        assertTrue(caseTypeLite.getEvents().isEmpty());
        assertNotNull(caseTypeLite.getStates());
        assertTrue(caseTypeLite.getStates().isEmpty());
    }

    @Test
    @DisplayName("Should create CaseTypeLite with empty events and states when empty lists")
    void shouldCreateCaseTypeLiteWithEmptyLists() {
        caseTypeDefinition.setEvents(new ArrayList<>());
        caseTypeDefinition.setStates(new ArrayList<>());

        CaseTypeLite caseTypeLite = new CaseTypeLite(caseTypeDefinition);

        assertNotNull(caseTypeLite);
        assertNotNull(caseTypeLite.getEvents());
        assertTrue(caseTypeLite.getEvents().isEmpty());
        assertNotNull(caseTypeLite.getStates());
        assertTrue(caseTypeLite.getStates().isEmpty());
    }

    @Test
    @DisplayName("Should create CaseTypeLite using JsonCreator constructor")
    void shouldCreateCaseTypeLiteUsingJsonCreator() {
        List<CaseEventDefinition> events = new ArrayList<>();
        CaseEventDefinition event = new CaseEventDefinition();
        event.setId("EVENT_1");
        event.setName("Event 1");
        events.add(event);

        List<CaseStateDefinition> states = new ArrayList<>();
        CaseStateDefinition state = new CaseStateDefinition();
        state.setId("STATE_1");
        state.setName("State 1");
        states.add(state);

        CaseTypeLite caseTypeLite = new CaseTypeLite(
            "TEST_ID",
            "Test Description",
            version,
            "Test Name",
            SecurityClassification.PUBLIC,
            events,
            states
        );

        assertNotNull(caseTypeLite);
        assertEquals("TEST_ID", caseTypeLite.getId());
        assertEquals("Test Description", caseTypeLite.getDescription());
        assertEquals("Test Name", caseTypeLite.getName());
        assertEquals(version, caseTypeLite.getVersion());
        assertEquals(SecurityClassification.PUBLIC, caseTypeLite.getSecurityClassification());
        assertNotNull(caseTypeLite.getEvents());
        assertNotNull(caseTypeLite.getStates());
    }

    @Test
    @DisplayName("Should create CaseTypeLite with null values using JsonCreator")
    void shouldCreateCaseTypeLiteWithNullValuesUsingJsonCreator() {
        CaseTypeLite caseTypeLite = new CaseTypeLite(
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertNotNull(caseTypeLite);
        assertNull(caseTypeLite.getId());
        assertNull(caseTypeLite.getDescription());
        assertNull(caseTypeLite.getName());
        assertNull(caseTypeLite.getVersion());
        assertNull(caseTypeLite.getSecurityClassification());
        assertNotNull(caseTypeLite.getEvents());
        assertTrue(caseTypeLite.getEvents().isEmpty());
        assertNotNull(caseTypeLite.getStates());
        assertTrue(caseTypeLite.getStates().isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple events and states")
    void shouldHandleMultipleEventsAndStates() {
        // Add multiple states
        for (int i = 1; i <= 3; i++) {
            CaseStateDefinition state = new CaseStateDefinition();
            state.setId("STATE_" + i);
            state.setName("State " + i);
            caseTypeDefinition.getStates().add(state);
        }

        // Add multiple events
        for (int i = 1; i <= 5; i++) {
            CaseEventDefinition event = new CaseEventDefinition();
            event.setId("EVENT_" + i);
            event.setName("Event " + i);
            caseTypeDefinition.getEvents().add(event);
        }

        CaseTypeLite caseTypeLite = new CaseTypeLite(caseTypeDefinition);

        assertEquals(5, caseTypeLite.getEvents().size());
        assertEquals(3, caseTypeLite.getStates().size());
    }

    @Test
    @DisplayName("Should preserve event preStates and accessControlLists using JsonCreator")
    void shouldPreserveEventPreStatesAndAccessControlLists() {
        // Test using JsonCreator constructor
        CaseStateDefinition state = new CaseStateDefinition();
        state.setId("STATE_1");
        state.setName("State 1");

        List<CaseStateDefinition> states = new ArrayList<>();
        states.add(state);

        CaseEventDefinition event = new CaseEventDefinition();
        event.setId("EVENT_1");
        event.setName("Event 1");
        event.setPreStates(Arrays.asList("PreState1", "PreState2"));

        List<AccessControlList> acls = new ArrayList<>();
        AccessControlList acl1 = new AccessControlList();
        acl1.setAccessProfile("profile1");
        acl1.setCreate(true);
        acls.add(acl1);

        AccessControlList acl2 = new AccessControlList();
        acl2.setAccessProfile("profile2");
        acl2.setRead(true);
        acls.add(acl2);

        event.setAccessControlLists(acls);

        List<CaseEventDefinition> events = new ArrayList<>();
        events.add(event);

        CaseTypeLite caseTypeLite = new CaseTypeLite(
            "TEST_ID",
            "Test Description",
            version,
            "Test Name",
            SecurityClassification.PUBLIC,
            events,
            states
        );

        assertEquals(1, caseTypeLite.getEvents().size());
        CaseEventLite eventLite = caseTypeLite.getEvents().get(0);
        assertEquals("EVENT_1", eventLite.getId());
        assertEquals("Event 1", eventLite.getName());
        assertEquals(Arrays.asList("PreState1", "PreState2"), eventLite.getPreStates());
        assertNotNull(eventLite.getAccessControlLists());
        assertEquals(2, eventLite.getAccessControlLists().size());
        assertEquals(1, caseTypeLite.getStates().size());
    }

    @Test
    @DisplayName("Should handle all SecurityClassification values")
    void shouldHandleAllSecurityClassificationValues() {
        for (SecurityClassification classification : SecurityClassification.values()) {
            caseTypeDefinition.setSecurityClassification(classification);
            CaseTypeLite caseTypeLite = new CaseTypeLite(caseTypeDefinition);
            assertEquals(classification, caseTypeLite.getSecurityClassification());
        }
    }
}
