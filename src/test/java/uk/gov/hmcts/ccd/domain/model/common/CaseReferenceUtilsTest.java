package uk.gov.hmcts.ccd.domain.model.common;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

class CaseReferenceUtilsTest {

    private final String validCaseWithHyphens = "1614-2496-6516-7002";
    private final String validCaseWithoutHyphens = "1614249665167002";

    @Test
    void testRemoveHyphens() {
        final String result = CaseReferenceUtils.removeHyphens(validCaseWithHyphens);
        assertEquals(result,validCaseWithoutHyphens);
    }

    @Test
    void testIsAValidCaseReferenceFormat() {
        assertEquals(true,CaseReferenceUtils.isAValidCaseReferenceFormat(validCaseWithHyphens));
        assertEquals(true,CaseReferenceUtils.isAValidCaseReferenceFormat(validCaseWithoutHyphens));
    }

    @Test
    void failIsAValidCaseReferenceFormat() {
        assertEquals(false,CaseReferenceUtils.isAValidCaseReferenceFormat("TETE"));
    }

    @Test
    void testGetFormatCaseReference() {
        final Optional<String> result = CaseReferenceUtils.getFormatCaseReference(Optional.of(validCaseWithHyphens));
        assertEquals(result,Optional.of(validCaseWithoutHyphens));
    }

    @Test
    void testGetFormatCaseReferenceForIncorrectValue() {
        final Optional<String> result = CaseReferenceUtils.getFormatCaseReference(Optional.of("TETE"));
        assertEquals(result,Optional.of(CaseReferenceUtils.EMPTY_CASE_REFERENCE));
    }
}
