package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

class CaseDocumentUtilsTest extends TestFixtures {
    private final CaseDocumentUtils underTest = new CaseDocumentUtils();

    @Test
    @SuppressWarnings({"ConstantConditions"})
    void testShouldRaiseExceptionWhenCaseDataIsNull() {
        // When
        final Throwable thrown = catchThrowable(() -> underTest.findDocumentNodes(null));

        // THEN
        assertThat(thrown)
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testShouldExtractAllDocumentNodes() throws Exception {
        // Given
        final Map<String, JsonNode> caseData = fromFileAsMap("case-data.json");
        final List<JsonNode> expectedDocumentNodes = fromFileAsList("case-document-nodes-without-hashtoken.json");

        // When
        final List<JsonNode> actualResult = underTest.findDocumentNodes(caseData);

        // Then
        assertThat(actualResult)
            .isNotNull()
            .hasSameElementsAs(expectedDocumentNodes);
    }

    @Test
    @SuppressWarnings({"ConstantConditions"})
    void testShouldRaiseExceptionWhenDocumentNodesIsNull() {
        // When
        final Throwable thrown = catchThrowable(() -> underTest.findDocumentsHashes(null));

        // THEN
        assertThat(thrown)
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testShouldGetDocumentHashTokens() throws Exception {
        // Given
        final Map<String, JsonNode> data = fromFileAsMap("new-document-with-hashtoken.json");

        final List<Tuple2<String, String>> expectedHashesMap = List.of(
            new Tuple2<>("0dfa903c-993d-4fa4-9bc4-b97d6352b862", null),
            new Tuple2<>("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292", null),
            new Tuple2<>("b5eb1f0e-64cd-4ccb-996a-6915c28fa65d", null),
            new Tuple2<>("8da17150-c001-47d7-bfeb-3dabed9e0976",
                "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886tytu"),
            new Tuple2<>("c1f160ca-cf52-4c0a-8376-3b51c340d00c",
                "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886617d")
        );

        // When
        final List<Tuple2<String, String>> actualResult = underTest.findDocumentsHashes(data);

        // Then
        assertThat(actualResult)
            .isNotNull()
            .hasSameElementsAs(expectedHashesMap);
    }

    @ParameterizedTest
    @MethodSource("provideNullListParameters")
    void testShouldRaiseExceptionWhenMapsAreNull(final List<Tuple2<String, String>> p1,
                                                 final List<Tuple2<String, String>> p2) {
        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.getTamperedHashes(p1, p2));

        // THEN
        assertThat(thrown)
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testShouldDetectTamperedHash() {
        // Given
        final List<Tuple2<String, String>> postCallbackHashes = List.of(
            new Tuple2<>("http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976",
                "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886tytu"),
            new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d",
                "4aca875b7160c9d24b6690276886617db5eb1f0e64cd4ccb996a6915c28fa65d")
        );

        // When
        final Set<String> actualResult = underTest.getTamperedHashes(DOCUMENT_HASH_PAIR_A, postCallbackHashes);

        // Then
        assertThat(actualResult)
            .singleElement()
            .isEqualTo("http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976");
    }

    @Test
    void testShouldDetectNoTempering() {
        // When
        final Set<String> actualResult = underTest.getTamperedHashes(DOCUMENT_HASH_PAIR_PRE, DOCUMENT_HASH_PAIR_POST);

        // Then
        assertThat(actualResult)
            .isNotNull()
            .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideHashTokenParameters")
    void testShouldBuildDocumentHashToken(final List<Tuple2<String, String>> p1,
                                          final List<Tuple2<String, String>> p2,
                                          final List<Tuple2<String, String>> p3,
                                          final List<DocumentHashToken> expectedHashTokens) {
        // When
        final List<DocumentHashToken> actualHashTokens = underTest.buildDocumentHashToken(p1, p2, p3);

        // Then
        assertThat(actualHashTokens)
            .isNotNull()
            .hasSameElementsAs(expectedHashTokens);
    }

    @ParameterizedTest
    @MethodSource("provideHashTokenValidationParameters")
    void testShouldValidateDocuments(final List<DocumentHashToken> documentHashTokens,
                                     final List<DocumentHashToken> expectedViolations) {
        // WHEN
        final List<DocumentHashToken> actualViolations = underTest.getViolatingDocuments(documentHashTokens);

        // THEN
        assertThat(actualViolations)
            .isNotNull()
            .hasSameElementsAs(expectedViolations);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideHashTokenParameters() {
        return Stream.of(
            Arguments.of(emptyList(), emptyList(), emptyList(), emptyList()),
            Arguments.of(emptyList(), emptyList(), DOCUMENT_NO_HASH_PAIR_A, List.of(DOC_A1, DOC_A2)),
            Arguments.of(emptyList(), emptyList(), DOCUMENT_HASH_PAIR_A, List.of(HASH_TOKEN_A1, HASH_TOKEN_A2)),
            Arguments.of(
                emptyList(),
                DOCUMENT_HASH_PAIR_A,
                DOCUMENT_NO_HASH_PAIR_A,
                List.of(HASH_TOKEN_A1, HASH_TOKEN_A2)
            ),
            Arguments.of(emptyList(), emptyList(), DOCUMENT_HASH_PAIR_B, List.of(HASH_TOKEN_B1, HASH_TOKEN_B2)),
            Arguments.of(
                List.of(new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d", null)),
                DOCUMENT_HASH_PAIR_A,
                DOCUMENT_HASH_PAIR_B,
                List.of(HASH_TOKEN_A1, HASH_TOKEN_A2, HASH_TOKEN_B1)
            ),
            Arguments.of(
                List.of(new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d", null)),
                DOCUMENT_HASH_PAIR_B,
                DOCUMENT_HASH_PAIR_A,
                List.of(HASH_TOKEN_A1, HASH_TOKEN_A2, HASH_TOKEN_B1)
            ),
            Arguments.of(
                List.of(new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d", null)),
                List.of(new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d", null)),
                DOCUMENT_HASH_PAIR_A,
                List.of(HASH_TOKEN_A1, HASH_TOKEN_A2)
            ),
            Arguments.of(
                List.of(new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d", null)),
                DOCUMENT_HASH_PAIR_B,
                List.of(new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d", null)),
                List.of(HASH_TOKEN_B1)
            ),
            Arguments.of(
                List.of(new Tuple2<>("http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d", null)),
                DOCUMENT_HASH_PAIR_B,
                DOCUMENT_HASH_PAIR_C,
                List.of(HASH_TOKEN_A1, HASH_TOKEN_A2, HASH_TOKEN_B1)
            )
        );
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideHashTokenValidationParameters() {
        return Stream.of(
            Arguments.of(emptyList(), emptyList()),
            Arguments.of(List.of(HASH_TOKEN_A1, HASH_TOKEN_A2), emptyList()),
            Arguments.of(List.of(HASH_TOKEN_B2), List.of(HASH_TOKEN_B2))
        );
    }
}
