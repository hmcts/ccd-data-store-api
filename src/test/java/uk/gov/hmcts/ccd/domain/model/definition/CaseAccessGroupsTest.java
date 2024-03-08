package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CaseAccessGroupsTest {

    private CaseAccessGroups caseAccessGroups =  new CaseAccessGroups();

    @BeforeEach
    void setUp() {

        MockitoAnnotations.initMocks(this);
        setupCaseAccessGroups();

    }

    @Test
    @DisplayName("Get caseAccessGroups")
    void caseAccessGroups() {

        assertAll(
            () -> assertEquals(caseAccessGroups.getCaseAccessGroupsList().size(), 1),
            () -> assertEquals(caseAccessGroups.getCaseAccessGroupsList().get(0).getCaseAccessGroupId(),
                "caseaccessGroupID"),
            () -> assertEquals(caseAccessGroups.getCaseAccessGroupsList().get(0).getCaseAccessGroupType(),
                "caseAccessGroupType1")
        );
    }


    private void setupCaseAccessGroups() {
        List<CaseAccessGroup> caseAccessGroupsList = new ArrayList<>();
        CaseAccessGroup caseAccessGroup = new CaseAccessGroup();
        caseAccessGroup.setCaseAccessGroupType("caseAccessGroupType1");
        caseAccessGroup.setCaseAccessGroupId("caseaccessGroupID");

        caseAccessGroupsList.add(caseAccessGroup);
        caseAccessGroups.setCaseAccessGroupsList(caseAccessGroupsList);

    }

    @Test
    public void testCaseAccessGroupsHaseCodeSame() {

        CaseAccessGroups p1 = caseAccessGroups;

        Map<CaseAccessGroups, String> map = new HashMap<>();
        map.put(p1, "dummy");

        CaseAccessGroups test1 = p1;

        assertTrue(p1.equals(test1));
        assertEquals(p1, test1);
    }

    @Test
    public void testCaseAccessGroupsHaseCodeNotSame() {

        CaseAccessGroups p1 = caseAccessGroups;

        Map<CaseAccessGroups, String> map = new HashMap<>();
        map.put(p1, "dummy");

        CaseAccessGroups test1 = p1;

        assertNotEquals(p1, map.get(test1));
    }

    @Test
    public void testCaseAccessGroupHaseCodeSame() {
        CaseAccessGroup p1 = caseAccessGroups.getCaseAccessGroupsList().get(0);

        Map<CaseAccessGroup, String> map = new HashMap<>();
        map.put(p1, "dummy");

        CaseAccessGroup test1 = p1;

        assertTrue(p1.equals(test1));
        assertEquals(map.get(p1), map.get(test1));
    }

    @Test
    public void testCaseAccessGroupHaseCodeNotSame() {
        CaseAccessGroup p1 = caseAccessGroups.getCaseAccessGroupsList().get(0);

        Map<CaseAccessGroup, String> map = new HashMap<>();
        map.put(p1, "dummy");

        CaseAccessGroup test1 = p1;
        assertNotEquals(p1, map.get(test1));
    }

    @Test
    public void testCaseAccessGroupsHaseCodeNull() {
        CaseAccessGroups p1 = new CaseAccessGroups();

        Map<CaseAccessGroups, String> map = new HashMap<>();
        map.put(p1, "dummy");

        CaseAccessGroups p2 = null;
        assertFalse(p1.equals(p2));
        assertNotEquals("dummy", map.get(p2));
    }

    @Test
    public void testCaseAccessGroupHaseCodeNull() {
        CaseAccessGroup p1 = new CaseAccessGroup();

        Map<CaseAccessGroup, String> map = new HashMap<>();
        map.put(p1, "dummy");

        Object p2 = null;
        assertFalse(p1.equals(p2));
        assertNotEquals("dummy", map.get(p2));
    }

}
