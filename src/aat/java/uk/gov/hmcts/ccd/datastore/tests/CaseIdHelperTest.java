package uk.gov.hmcts.ccd.datastore.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.befta.exception.FunctionalTestException;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.ccd.datastore.util.CaseIdHelper.hypheniseACaseId;

@DisplayName("CaseIdHelperTest")
class CaseIdHelperTest {

    @Test
    void testHypheniseACaseId() {
        String expectedResult = "1609-2434-4756-9251";
        final String result = hypheniseACaseId("1609243447569251");
        assertEquals(result,expectedResult);
    }

    @Test
    void failHypheniseACaseId() {
        assertThrows(FunctionalTestException.class, () -> hypheniseACaseId("BLAH"));
    }
}
