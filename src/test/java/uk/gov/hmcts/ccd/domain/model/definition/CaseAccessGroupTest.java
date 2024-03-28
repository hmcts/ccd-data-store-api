package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseAccessGroupTest {

    @BeforeEach
    void setUp() {

        MockitoAnnotations.initMocks(this);
    }

    @Test
   void testCaseAccessGroupHaseCodeSame() {
        CaseAccessGroup p1 = new CaseAccessGroup();

        Map<CaseAccessGroup, String> map = new HashMap<>();
        map.put(p1, "dummy");

        CaseAccessGroup test1 = p1;

        assertTrue(p1.equals(test1));
        assertEquals(p1, test1);
    }

    @Test
    void testCaseAccessGroupHaseCodeNull() {
        CaseAccessGroup p1 = new CaseAccessGroup();

        Map<CaseAccessGroup, String> map = new HashMap<>();
        map.put(p1, "dummy");

        Object p2 = null;
        assertNotEquals(p1,p2);
    }

}
