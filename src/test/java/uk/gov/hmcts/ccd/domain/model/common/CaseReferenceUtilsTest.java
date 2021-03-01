package uk.gov.hmcts.ccd.domain.model.common;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class CaseReferenceUtilsTest {

    private final String validCaseWithHyphens = "1614-2496-6516-7002";
    private final String validCaseWithoutHyphens = "1614249665167002";

    @Test
    void testRemoveHyphens() {
        final String result = CaseReferenceUtils.removeHyphens(validCaseWithHyphens);
        assertEquals(result, validCaseWithoutHyphens);
    }

    @Test
    void testIsAValidCaseReferenceFormat() {
        assertEquals(true, CaseReferenceUtils.isAValidCaseReference(validCaseWithHyphens));
        assertEquals(true, CaseReferenceUtils.isAValidCaseReference(validCaseWithoutHyphens));
    }

    @Test
    void failIsAValidCaseReferenceFormat() {
        assertEquals(false, CaseReferenceUtils.isAValidCaseReference("TETE"));
    }
}
