package uk.gov.hmcts.ccd.domain.model.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CaseReferenceUtilsTest {

    private final String validCaseWithHyphens = "1614-2496-6516-7002";
    private final String validCaseWithoutHyphens = "1614249665167002";

    @ParameterizedTest
    @CsvSource({
        "'1614-2496-6516-7002', '1614249665167002', true",
        "'1614249665167002',    '1614249665167002', true",
        "'TETE',                'TETE',             false"
    })
    void shouldValidateCaseReferenceAndRemoveHyphens(
        String caseReference,
        String expectedWithoutHyphens,
        boolean expectedValid
    ) {
        assertThat(CaseReferenceUtils.removeHyphens(caseReference))
            .isEqualTo(expectedWithoutHyphens);

        assertThat(CaseReferenceUtils.isAValidCaseReference(caseReference))
            .isEqualTo(expectedValid);
    }
}
