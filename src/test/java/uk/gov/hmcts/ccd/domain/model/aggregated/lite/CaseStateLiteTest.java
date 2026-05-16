package uk.gov.hmcts.ccd.domain.model.aggregated.lite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("CaseStateLite Test")
class CaseStateLiteTest {

    private CaseStateLite caseStateLite;

    @BeforeEach
    void setUp() {
        caseStateLite = new CaseStateLite();
    }

    @Test
    @DisplayName("Should create CaseStateLite with default values")
    void shouldCreateCaseStateLiteWithDefaultValues() {
        assertNotNull(caseStateLite);
        assertNull(caseStateLite.getId());
        assertNull(caseStateLite.getName());
        assertNull(caseStateLite.getDescription());
    }

    @Test
    @DisplayName("Should set and get id")
    void shouldSetAndGetId() {
        String id = "STATE_1";
        caseStateLite.setId(id);
        assertEquals(id, caseStateLite.getId());
    }

    @Test
    @DisplayName("Should set and get name")
    void shouldSetAndGetName() {
        String name = "Test State";
        caseStateLite.setName(name);
        assertEquals(name, caseStateLite.getName());
    }

    @Test
    @DisplayName("Should set and get description")
    void shouldSetAndGetDescription() {
        String description = "Test State Description";
        caseStateLite.setDescription(description);
        assertEquals(description, caseStateLite.getDescription());
    }

    @Test
    @DisplayName("Should set all properties")
    void shouldSetAllProperties() {
        String id = "STATE_1";
        String name = "Test State";
        String description = "Test Description";

        caseStateLite.setId(id);
        caseStateLite.setName(name);
        caseStateLite.setDescription(description);

        assertEquals(id, caseStateLite.getId());
        assertEquals(name, caseStateLite.getName());
        assertEquals(description, caseStateLite.getDescription());
    }

    @Test
    @DisplayName("Should create copy of CaseStateLite")
    void shouldCreateCopyOfCaseStateLite() {
        caseStateLite.setId("STATE_1");
        caseStateLite.setName("Test State");
        caseStateLite.setDescription("Test Description");

        CaseStateLite copy = caseStateLite.createCopy();

        assertNotNull(copy);
        assertNotSame(caseStateLite, copy);
        assertEquals(caseStateLite.getId(), copy.getId());
        assertEquals(caseStateLite.getName(), copy.getName());
        assertEquals(caseStateLite.getDescription(), copy.getDescription());
    }

    @Test
    @DisplayName("Should create copy with null values")
    void shouldCreateCopyWithNullValues() {
        caseStateLite.setId(null);
        caseStateLite.setName(null);
        caseStateLite.setDescription(null);

        CaseStateLite copy = caseStateLite.createCopy();

        assertNotNull(copy);
        assertNull(copy.getId());
        assertNull(copy.getName());
        assertNull(copy.getDescription());
    }

    @Test
    @DisplayName("Should create independent copy - modifying copy should not affect original")
    void shouldCreateIndependentCopy() {
        caseStateLite.setId("STATE_1");
        caseStateLite.setName("Original Name");
        caseStateLite.setDescription("Original Description");

        CaseStateLite copy = caseStateLite.createCopy();
        copy.setName("Modified Name");
        copy.setDescription("Modified Description");

        assertEquals("Original Name", caseStateLite.getName());
        assertEquals("Original Description", caseStateLite.getDescription());
        assertEquals("Modified Name", copy.getName());
        assertEquals("Modified Description", copy.getDescription());
    }

    @Test
    @DisplayName("Should handle empty strings")
    void shouldHandleEmptyStrings() {
        caseStateLite.setId("");
        caseStateLite.setName("");
        caseStateLite.setDescription("");

        assertEquals("", caseStateLite.getId());
        assertEquals("", caseStateLite.getName());
        assertEquals("", caseStateLite.getDescription());
    }
}
