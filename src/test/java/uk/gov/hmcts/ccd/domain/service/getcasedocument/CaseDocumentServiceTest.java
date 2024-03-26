package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CaseDocumentServiceTest extends TestFixtures {
    @Mock
    private CaseService caseService;

    @Mock
    private CaseDocumentUtils documentUtils;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseDocumentAmApiClient caseDocumentAmApiClient;

    @InjectMocks
    private CaseDocumentService underTest;

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASE_DETAIL_FIELD = "dataTestField1";

    private final String urlGoogle = "https://www.google.com";
    private final String urlYahoo = "https://www.yahoo.com";
    private final String urlMicrosoft = "https://www.microsoft.com";
    private final String urlElastic = "https://www.elastic.com";
    private final String urlApple = "https://www.apple.com";

    @Test
    void testShouldReturnClonedCaseDetailsWithoutHashes() throws Exception {
        // Given
        final Map<String, JsonNode> data = fromFileAsMap("new-document-with-hashtoken.json");
        final CaseDetails caseDetails = buildCaseDetails(data);
        final Map<String, JsonNode> dataWithoutHashes = fromFileAsMap("new-document-with-removed-hashtoken.json");

        doReturn(caseDetails).when(caseService).clone(caseDetails);
        doCallRealMethod().when(documentUtils).findDocumentNodes(anyMap());

        // When
        final CaseDetails actualClonedCaseDetails = underTest.stripDocumentHashes(caseDetails);

        // Then
        verify(caseService).clone(caseDetails);
        verify(documentUtils, times(2)).findDocumentNodes(anyMap());

        assertThat(actualClonedCaseDetails)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getJurisdiction()).isEqualTo(JURISDICTION_ID);
                assertThat(x.getReference()).isEqualTo(REFERENCE);
                assertThat(x.getState()).isEqualTo(STATE);
                assertThat(x.getData()).isEqualTo(dataWithoutHashes);
            });
    }

    @Test
    void testShouldReturnCloneOfOriginalCaseDetailsWhenNoDocumentsPresent() throws Exception {
        // Given
        final Map<String, JsonNode> data = fromFileAsMap("text-type-case-field.json");
        final CaseDetails caseDetails = buildCaseDetails(data);

        doReturn(true).when(applicationParams).isDocumentHashCloneEnabled();
        doReturn(caseDetails).when(caseService).clone(caseDetails);
        doCallRealMethod().when(documentUtils).findDocumentNodes(anyMap());

        // When
        final CaseDetails actualClonedCaseDetails = underTest.stripDocumentHashes(caseDetails);

        // Then
        verify(caseService).clone(caseDetails);
        verify(documentUtils).findDocumentNodes(anyMap());

        assertThat(actualClonedCaseDetails)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getJurisdiction()).isEqualTo(JURISDICTION_ID);
                assertThat(x.getReference()).isEqualTo(REFERENCE);
                assertThat(x.getState()).isEqualTo(STATE);
                assertThat(x.getData()).isEqualTo(data);
            });
    }

    @Test
    void testShouldRaiseExceptionWhenHashTokensAreMissing() {
        // GIVEN
        doReturn(true).when(applicationParams).isDocumentHashCheckingEnabled();
        doReturn(List.of(HASH_TOKEN_B2)).when(documentUtils).getViolatingDocuments(anyList());

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.validate(List.of(HASH_TOKEN_B2)));

        // THEN
        verify(documentUtils).getViolatingDocuments(anyList());

        assertThat(thrown)
            .isInstanceOf(ValidationException.class)
            .hasMessageStartingWith("Document hashTokens are missing for the documents: ");
    }

    @Test
    void testShouldBuildValidDocumentHashTokens() {
        // Given
        final Map<String, JsonNode> preCallbackCaseData = emptyMap();
        final Map<String, JsonNode> postCallbackCaseData = emptyMap();

        doReturn(emptyList(), emptyList()).when(documentUtils).findDocumentsHashes(anyMap());
        doReturn(emptySet()).when(documentUtils).getTamperedHashes(anyList(), anyList());
        doReturn(List.of(HASH_TOKEN_A1, HASH_TOKEN_A2))
            .when(documentUtils).buildDocumentHashToken(anyList(), anyList(), anyList());
        doReturn(true).when(applicationParams).isDocumentHashCheckingEnabled();
        doReturn(emptyList()).when(documentUtils).getViolatingDocuments(anyList());

        // When
        final List<DocumentHashToken> result = underTest.extractDocumentHashToken(
            preCallbackCaseData,
            postCallbackCaseData
        );

        // Then
        assertThat(result)
            .isNotNull()
            .hasSameElementsAs(List.of(HASH_TOKEN_A1, HASH_TOKEN_A2));

        verify(documentUtils, times(3)).findDocumentsHashes(anyMap());
        verify(documentUtils).getTamperedHashes(anyList(), anyList());
        verify(documentUtils).buildDocumentHashToken(anyList(), anyList(), anyList());
        verify(documentUtils).getViolatingDocuments(anyList());
        verify(applicationParams).isDocumentHashCheckingEnabled();
    }

    @Test
    void testShouldRaiseValidationException() {
        // Given
        final Map<String, JsonNode> preCallbackCaseData = emptyMap();
        final Map<String, JsonNode> postCallbackCaseData = emptyMap();

        doReturn(emptyList(), emptyList()).when(documentUtils).findDocumentsHashes(anyMap());
        doReturn(emptySet()).when(documentUtils).getTamperedHashes(anyList(), anyList());
        doReturn(List.of(HASH_TOKEN_B1, HASH_TOKEN_B2))
            .when(documentUtils).buildDocumentHashToken(anyList(), anyList(), anyList());
        doReturn(true).when(applicationParams).isDocumentHashCheckingEnabled();
        doReturn(List.of(HASH_TOKEN_B2)).when(documentUtils).getViolatingDocuments(anyList());

        // When
        final Throwable thrown = catchThrowable(() -> underTest.extractDocumentHashToken(
            preCallbackCaseData,
            postCallbackCaseData
        ));

        // Then
        assertThat(thrown)
            .isInstanceOf(ValidationException.class)
            .hasMessageStartingWith("Document hashTokens are missing for the documents: ");

        verify(documentUtils, times(3)).findDocumentsHashes(anyMap());
        verify(documentUtils).getTamperedHashes(anyList(), anyList());
        verify(documentUtils).buildDocumentHashToken(anyList(), anyList(), anyList());
        verify(documentUtils).getViolatingDocuments(anyList());
        verify(applicationParams).isDocumentHashCheckingEnabled();
    }

    @Test
    void testShouldRaiseServiceException() {
        // Given
        final Map<String, JsonNode> preCallbackCaseData = emptyMap();
        final Map<String, JsonNode> postCallbackCaseData = emptyMap();

        doReturn(emptyList(), emptyList()).when(documentUtils).findDocumentsHashes(anyMap());
        doReturn(Set.of("tampered")).when(documentUtils).getTamperedHashes(anyList(), anyList());

        // When
        final Throwable thrown = catchThrowable(() -> underTest.extractDocumentHashToken(
            preCallbackCaseData,
            postCallbackCaseData
        ));

        // Then
        assertThat(thrown)
            .isInstanceOf(ServiceException.class)
            .hasMessageStartingWith("Callback attempted to change the hashToken of the following documents:");

        verify(documentUtils, times(3)).findDocumentsHashes(anyMap());
        verify(documentUtils).getTamperedHashes(anyList(), anyList());
        verifyNoMoreInteractions(documentUtils);
    }

    @Test
    void testShouldApplyCaseDocumentPatch() {
        // Given
        doReturn(true).when(applicationParams).isAttachDocumentEnabled();
        final List<DocumentHashToken> documentHashTokens = List.of(HASH_TOKEN_A1, HASH_TOKEN_A2);
        doNothing().when(caseDocumentAmApiClient).applyPatch(any(CaseDocumentsMetadata.class));

        // When
        underTest.attachCaseDocuments(CASE_REFERENCE, CASE_TYPE_ID, JURISDICTION_ID, documentHashTokens);

        // Then
        verify(caseDocumentAmApiClient).applyPatch(any(CaseDocumentsMetadata.class));
    }

    @Test
    void testShouldNotApplyCaseDocumentPatchWhenFeaturedDisabled() {
        // Given
        doReturn(false).when(applicationParams).isAttachDocumentEnabled();
        final List<DocumentHashToken> documentHashTokens = List.of(HASH_TOKEN_A1, HASH_TOKEN_A2);

        // When
        underTest.attachCaseDocuments(CASE_REFERENCE, CASE_TYPE_ID, JURISDICTION_ID, documentHashTokens);

        // Then
        verify(caseDocumentAmApiClient, never()).applyPatch(any(CaseDocumentsMetadata.class));
    }

    @Test
    void testShouldNotApplyPatchWhenNoDocumentHashes() {
        // When
        underTest.attachCaseDocuments(CASE_REFERENCE, CASE_TYPE_ID, JURISDICTION_ID, emptyList());

        // Then
        verifyNoMoreInteractions(caseDocumentAmApiClient);
    }

    @ParameterizedTest
    @MethodSource("provideValidHashTokenParameters")
    void testShouldCheckForViolations(final List<DocumentHashToken> documentHashTokens) {
        // GIVEN
        doReturn(true).when(applicationParams).isDocumentHashCheckingEnabled();
        doReturn(emptyList()).when(documentUtils).getViolatingDocuments(anyList());

        // WHEN/THEN
        assertThatCode(() -> underTest.validate(documentHashTokens))
            .doesNotThrowAnyException();
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideValidHashTokenParameters() {
        return Stream.of(
            Arguments.of(emptyList()),
            Arguments.of(List.of(HASH_TOKEN_A1, HASH_TOKEN_A2))
        );
    }

    @Test
    void testShouldNotCheckDocumentsForHashToken() {
        // GIVEN
        doReturn(false).when(applicationParams).isDocumentHashCheckingEnabled();

        // WHEN/THEN
        assertThatCode(() -> underTest.validate(emptyList()))
            .doesNotThrowAnyException();

        verifyNoMoreInteractions(documentUtils);
    }

}
