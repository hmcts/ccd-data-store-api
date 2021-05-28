package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
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
import static java.util.Collections.emptyMap;
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
        final Map<String, JsonNode> caseData = loadDataAsMap("case-data.json");
        final List<JsonNode> expectedDocumentNodes = loadDataAsList("expected-document-nodes.json");

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
        final Throwable thrown = catchThrowable(() -> underTest.extractDocumentsHashes(null));

        // THEN
        assertThat(thrown)
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testShouldBuildEmptyDocumentHashMap() throws Exception {
        // Given
        final Map<String, JsonNode> caseData = loadDataAsMap("case-data.json");

        // When
        final Map<String, String> actualResult = underTest.extractDocumentsHashes(caseData);

        // Then
        assertThat(actualResult)
            .isNotNull()
            .isEmpty();
    }

    @Test
    void testShouldBuildDocumentHashMap() throws Exception {
        // Given
        final Map<String, JsonNode> data = loadDataAsMap("new-document-with-hashtoken.json");
        final Map<String, String> expectedHashesMap = Map.of(
            "http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886tytu",
            "http://dm-store:8080/documents/c1f160ca-cf52-4c0a-8376-3b51c340d00c",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886617d"
        );

        // When
        final Map<String, String> actualResult = underTest.extractDocumentsHashes(data);

        // Then
        assertThat(actualResult)
            .isNotNull()
            .containsExactlyInAnyOrderEntriesOf(expectedHashesMap);
    }

    @ParameterizedTest
    @MethodSource("provideNullMapParameters")
    void testShouldRaiseExceptionWhenMapsAreNull(final Map<String, String> m1, final Map<String, String> m2) {
        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.getTamperedHashes(m1, m2));

        // THEN
        assertThat(thrown)
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testShouldDetectTamperedHash() {
        // Given
        final Map<String, String> preCallbackHashes = Map.of(
            "http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886tytu",
            "http://dm-store:8080/documents/c1f160ca-cf52-4c0a-8376-3b51c340d00c",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886617d"
        );
        final Map<String, String> postCallbackHashes = Map.of(
            "http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886tytu",
            "http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d",
            "4aca875b7160c9d24b6690276886617db5eb1f0e64cd4ccb996a6915c28fa65d"
        );

        // When
        final Set<String> actualResult = underTest.getTamperedHashes(preCallbackHashes, postCallbackHashes);

        // Then
        assertThat(actualResult)
            .singleElement()
            .isEqualTo("http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976");
    }

    @Test
    void testShouldDetectNoTempering() {
        // Given
        final Map<String, String> preCallbackHashes = Map.of(
            "http://dm-store:8080/documents/8da17150-c001-47d7-bfeb-3dabed9e0976",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886tytu",
            "http://dm-store:8080/documents/c1f160ca-cf52-4c0a-8376-3b51c340d00c",
            "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886617d"
        );
        final Map<String, String> postCallbackHashes = Map.of(
            "http://dm-store:8080/documents/ed9e0976-c001-47d7-bfeb-3dab8da17150",
            "60c9d24b6690276886tytu36fc7aa586a54bffc2982ed490c4503f4aca875b71",
            "http://dm-store:8080/documents/b5eb1f0e-64cd-4ccb-996a-6915c28fa65d",
            "4aca875b7160c9d24b6690276886617db5eb1f0e64cd4ccb996a6915c28fa65d"
        );

        // When
        final Set<String> actualResult = underTest.getTamperedHashes(preCallbackHashes, postCallbackHashes);

        // Then
        assertThat(actualResult)
            .isNotNull()
            .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideHashTokenParameters")
    void testShouldBuildDocumentHashToken(final Map<String, String> m1,
                                          final Map<String, String> m2,
                                          final List<DocumentHashToken> expectedHashTokens) {
        // When
        final List<DocumentHashToken> actualHashTokens = underTest.buildDocumentHashToken(m1, m2);

        // Then
        assertThat(actualHashTokens)
            .isNotNull()
            .hasSameElementsAs(expectedHashTokens);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideHashTokenParameters() {
        return Stream.of(
            Arguments.of(emptyMap(), emptyMap(), emptyList()),
            Arguments.of(MAP_A, emptyMap(), List.of(HASH_TOKEN_A)),
            Arguments.of(emptyMap(), MAP_B, List.of(HASH_TOKEN_B)),
            Arguments.of(MAP_A, MAP_B, List.of(HASH_TOKEN_A, HASH_TOKEN_B))
        );
    }
}
