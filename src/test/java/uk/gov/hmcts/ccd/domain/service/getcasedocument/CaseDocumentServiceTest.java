package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.mockito.Mockito.verifyZeroInteractions;

@ExtendWith(MockitoExtension.class)
class CaseDocumentServiceTest extends TestFixtures {
    private static final String STATE = "CreatedState";

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
                assertThat(x.getJurisdiction()).isEqualTo(JURISDICTION);
                assertThat(x.getReference()).isEqualTo(REFERENCE);
                assertThat(x.getState()).isEqualTo(STATE);
                assertThat(x.getData()).isEqualTo(dataWithoutHashes);
            });
    }

    @Test
    void testShouldReturnOriginalCaseDetailsWhenNoDocumentsPresent() throws Exception {
        // Given
        final Map<String, JsonNode> data = fromFileAsMap("text-type-case-field.json");
        final CaseDetails caseDetails = buildCaseDetails(data);

        doCallRealMethod().when(documentUtils).findDocumentNodes(anyMap());

        // When
        final CaseDetails actualClonedCaseDetails = underTest.stripDocumentHashes(caseDetails);

        // Then
        verifyZeroInteractions(caseService);
        verify(documentUtils).findDocumentNodes(anyMap());

        assertThat(actualClonedCaseDetails)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getJurisdiction()).isEqualTo(JURISDICTION);
                assertThat(x.getReference()).isEqualTo(REFERENCE);
                assertThat(x.getState()).isEqualTo(STATE);
                assertThat(x.getData()).isEqualTo(data);
            });
    }

    @Test
    void testShouldRaiseServiceException() {
        // Given
        final CaseDetails preCallbackCaseDetails = buildCaseDetails(emptyMap());
        final CaseDetails postCallbackCaseDetails = buildCaseDetails(emptyMap());

        doReturn(emptyMap(), emptyMap()).when(documentUtils).extractDocumentsHashes(anyMap());
        doReturn(Set.of("tampered")).when(documentUtils).getTamperedHashes(anyMap(), anyMap());

        // When
        final Throwable thrown = catchThrowable(() -> underTest.attachCaseDocuments(
            preCallbackCaseDetails,
            postCallbackCaseDetails)
        );

        // Then
        assertThat(thrown)
            .isInstanceOf(ServiceException.class)
            .hasMessageStartingWith("call back attempted to change the hashToken of the following documents:");

        verify(documentUtils, never()).buildDocumentHashToken(anyMap(), anyMap());
    }

    @Test
    void testShouldApplyCaseDocumentPatch() {
        // Given
        final CaseDetails preCallbackCaseDetails = buildCaseDetails(emptyMap());
        final CaseDetails postCallbackCaseDetails = buildCaseDetails(emptyMap());

        doReturn(emptyMap(), emptyMap()).when(documentUtils).extractDocumentsHashes(anyMap());
        doReturn(emptySet()).when(documentUtils).getTamperedHashes(anyMap(), anyMap());
        doReturn(List.of(HASH_TOKEN_A, HASH_TOKEN_B)).when(documentUtils).buildDocumentHashToken(anyMap(), anyMap());
        doNothing().when(caseDocumentAmApiClient).applyPatch(any(CaseDocumentsMetadata.class));

        // When
        underTest.attachCaseDocuments(preCallbackCaseDetails, postCallbackCaseDetails);

        // Then
        verify(documentUtils, times(2)).extractDocumentsHashes(anyMap());
        verify(documentUtils).getTamperedHashes(anyMap(), anyMap());
        verify(documentUtils).buildDocumentHashToken(anyMap(), anyMap());
        verify(caseDocumentAmApiClient).applyPatch(any(CaseDocumentsMetadata.class));
    }

    @Test
    void testShouldNotApplyPatchWhenNoDocumentHashes() {
        // Given
        final CaseDetails preCallbackCaseDetails = buildCaseDetails(emptyMap());
        final CaseDetails postCallbackCaseDetails = buildCaseDetails(emptyMap());

        doReturn(emptyMap(), emptyMap()).when(documentUtils).extractDocumentsHashes(anyMap());
        doReturn(emptySet()).when(documentUtils).getTamperedHashes(anyMap(), anyMap());
        doReturn(emptyList()).when(documentUtils).buildDocumentHashToken(anyMap(), anyMap());

        // When
        underTest.attachCaseDocuments(preCallbackCaseDetails, postCallbackCaseDetails);

        // Then
        verifyZeroInteractions(caseDocumentAmApiClient);
    }

    @ParameterizedTest
    @MethodSource("provideDocumentNodesParameters")
    void testShouldFindNoViolations(final Map<String, JsonNode> originalCaseData,
                                    final Map<String, JsonNode> caseDataAfterCallback,
                                    final List<JsonNode> documentNodes) {
        // GIVEN
        doReturn(documentNodes).when(documentUtils).findDocumentNodes(anyMap());
        doReturn(emptyList()).when(documentUtils).getViolatingDocuments(anyList(), anyList());
        doReturn(true).when(applicationParams).isDocumentHashCheckingEnabled();

        // WHEN/THEN
        assertThatCode(() -> underTest.validate(originalCaseData, caseDataAfterCallback))
            .doesNotThrowAnyException();
    }

    @Test
    void testShouldRaiseExceptionWhenPreCallbackDocumentsHaveNoHashes() throws Exception {
        // GIVEN
        final Map<String, JsonNode> data = emptyMap();
        final List<JsonNode> documentNodes = fromFileAsList("case-document-nodes-without-hashtoken.json");
        doReturn(documentNodes, emptyList()).when(documentUtils).findDocumentNodes(anyMap());
        doReturn(documentNodes).when(documentUtils).getViolatingDocuments(anyList(), anyList());
        doReturn(true).when(applicationParams).isDocumentHashCheckingEnabled();

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.validate(data, emptyMap()));

        // THEN
        verify(documentUtils, times(2)).findDocumentNodes(anyMap());
        verify(documentUtils).getViolatingDocuments(anyList(), anyList());

        assertThat(thrown)
            .isInstanceOf(ValidationException.class)
            .hasMessageStartingWith("Some message");
    }

    @Test
    void testShouldRaiseExceptionWhenPostCallbackDocumentsHaveNoHashes() throws Exception {
        // GIVEN
        final Map<String, JsonNode> data = emptyMap();
        final List<JsonNode> documentNodes = fromFileAsList("case-document-nodes-without-hashtoken.json");
        doReturn(emptyList(), documentNodes).when(documentUtils).findDocumentNodes(anyMap());
        doReturn(documentNodes).when(documentUtils).getViolatingDocuments(anyList(), anyList());
        doReturn(true).when(applicationParams).isDocumentHashCheckingEnabled();

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.validate(emptyMap(), data));

        // THEN
        verify(documentUtils, times(2)).findDocumentNodes(anyMap());
        verify(documentUtils).getViolatingDocuments(anyList(), anyList());

        assertThat(thrown)
            .isInstanceOf(ValidationException.class)
            .hasMessageStartingWith("Some message");
    }

    @Test
    void testShouldNotCheckDocumentsForHashToken() {
        // GIVEN
        doReturn(false).when(applicationParams).isDocumentHashCheckingEnabled();

        // WHEN/THEN
        assertThatCode(() -> underTest.validate(emptyMap(), emptyMap()))
            .doesNotThrowAnyException();

        verifyZeroInteractions(documentUtils);
    }

    private CaseDetails buildCaseDetails(final Map<String, JsonNode> data) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setReference(REFERENCE);
        caseDetails.setState(STATE);

        caseDetails.setData(data);

        return caseDetails;
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideDocumentNodesParameters() throws Exception {
        final Map<String, JsonNode> preCallbackData = fromFileAsMap("A-case-data-with-hashtoken.json");
        final List<JsonNode> preCallbackDocumentNodes = fromFileAsList("A-document-nodes.json");

        final Map<String, JsonNode> postCallbackData = fromFileAsMap("SubmitTransactionDocumentUpload.json");
        final List<JsonNode> documentNodes = fromFileAsList("case-document-nodes-with-hashtoken.json");

        return Stream.of(
            Arguments.of(emptyMap(), emptyMap(), emptyList()),
            Arguments.of(preCallbackData, emptyMap(), preCallbackDocumentNodes),
            Arguments.of(emptyMap(), postCallbackData, documentNodes),
            Arguments.of(preCallbackData, postCallbackData, documentNodes)
        );
    }
}
