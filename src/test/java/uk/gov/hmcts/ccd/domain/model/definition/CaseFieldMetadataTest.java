package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

import static org.assertj.core.api.Assertions.assertThat;

class CaseFieldMetadataTest {

    @ParameterizedTest
    @NullSource
    void testShouldReturnNull(final String input) {
        final CaseFieldMetadata underTest = new CaseFieldMetadata(input, null);

        final String actualResult = underTest.getPathAsJsonPath();

        assertThat(actualResult)
            .isNull();
    }

    @ParameterizedTest
    @EmptySource
    void testShouldReturnEmptyString(final String input) {
        final CaseFieldMetadata underTest = new CaseFieldMetadata(input, null);

        final String actualResult = underTest.getPathAsJsonPath();

        assertThat(actualResult)
            .isNotNull()
            .isEqualTo(input);
    }

    @ParameterizedTest
    @CsvSource({"state,$.state", "nationalityProof.documentEvidence,$.nationalityProof.documentEvidence",
        "extraDocUploadList.0,$.extraDocUploadList[0].value",
        "state.0.partyDetail.1.type,$.state[0].value.partyDetail[1].value.type"})
    void testShouldTransformToJsonPath(final String input, final String expectedJsonPath) {
        final CaseFieldMetadata underTest = new CaseFieldMetadata(input, null);

        final String actualResult = underTest.getPathAsJsonPath();

        assertThat(actualResult)
            .isNotNull()
            .isEqualTo(expectedJsonPath);
    }

    @ParameterizedTest
    @NullSource
    void testGetPathAsAttributePathShouldReturnNull(final String input) {
        final CaseFieldMetadata underTest = new CaseFieldMetadata(input, null);

        final String actualResult = underTest.getPathAsAttributePath();

        assertThat(actualResult)
            .isNull();
    }

    @ParameterizedTest
    @EmptySource
    void testGetPathAsAttributePathShouldReturnEmptyString(final String input) {
        final CaseFieldMetadata underTest = new CaseFieldMetadata(input, null);

        final String actualResult = underTest.getPathAsAttributePath();

        assertThat(actualResult)
            .isNotNull()
            .isEqualTo(input);
    }

    @ParameterizedTest
    @CsvSource({"state,state", "nationalityProof.documentEvidence,nationalityProof.documentEvidence",
        "extraDocUploadList.0,extraDocUploadList[0].value",
        "state.0.partyDetail.1.type,state[0].value.partyDetail[1].value.type"})
    void testShouldTransformToAttributePath(final String input, final String expectedAttributePath) {
        final CaseFieldMetadata underTest = new CaseFieldMetadata(input, null);

        final String actualResult = underTest.getPathAsAttributePath();

        assertThat(actualResult)
            .isNotNull()
            .isEqualTo(expectedAttributePath);
    }
}
