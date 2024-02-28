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
            () -> assertEquals(caseAccessGroups.getCaseAccessGroups().size(), 1),
            () -> assertEquals(caseAccessGroups.getCaseAccessGroups().get(0).getCaseAccessGroupId(),
                "caseaccessGroupID"),
            () -> assertEquals(caseAccessGroups.getCaseAccessGroups().get(0).getCaseAccessGroupType(),
                "caseAccessGroupType1")
        );
    }


    private void setupCaseAccessGroups() {
        List<CaseAccessGroup> caseAccessGroupsList = new ArrayList<>();
        CaseAccessGroup caseAccessGroup = new CaseAccessGroup();
        caseAccessGroup.setCaseAccessGroupType("caseAccessGroupType1");
        caseAccessGroup.setCaseAccessGroupId("caseaccessGroupID");

        caseAccessGroupsList.add(caseAccessGroup);
        caseAccessGroups.setCaseAccessGroups(caseAccessGroupsList);

    }

    @Test
    public void testCaseAccessHaseCode() {
        CaseAccessGroups p1 = new CaseAccessGroups();

        Map<CaseAccessGroups, String> map = new HashMap<>();
        map.put(p1, "dummy");

        CaseAccessGroups p2 = new CaseAccessGroups();
        assertEquals("dummy", map.get(p2));
    }

}
