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

    @Test
    void testShouldReturnEmptyListWhenDocumentHashesIsEmpty() {
        // Given
        final Map<String, JsonNode> savedData = emptyMap();

        // When
        final List<DocumentHashToken> result = underTest.filterDocumentHashesAgainstSavedData(emptyList(), savedData);

        // Then
        assertThat(result).isEmpty();
        verifyNoMoreInteractions(documentUtils);
    }

    @Test
    void testShouldReturnEmptyListWhenNoDocumentsInSavedData() {
        // Given
        final List<DocumentHashToken> documentHashes = List.of(HASH_TOKEN_A1, HASH_TOKEN_A2);

        doReturn(emptySet()).when(documentUtils).findDocumentIds(anyMap());

        // When
        final List<DocumentHashToken> result = underTest.filterDocumentHashesAgainstSavedData(documentHashes, emptyMap());

        // Then
        assertThat(result).isEmpty();
        verify(documentUtils).findDocumentIds(anyMap());
    }

    @Test
    void testShouldReturnOnlyDocumentHashesPresentInSavedData() {
        // Given
        final List<DocumentHashToken> documentHashes = List.of(HASH_TOKEN_A1, HASH_TOKEN_A2);
        final Set<String> savedDocumentIds = Set.of(HASH_TOKEN_A1.getId());

        doReturn(savedDocumentIds).when(documentUtils).findDocumentIds(anyMap());

        // When
        final List<DocumentHashToken> result = underTest.filterDocumentHashesAgainstSavedData(documentHashes, emptyMap());

        // Then
        assertThat(result)
            .hasSize(1)
            .containsExactly(HASH_TOKEN_A1);
        verify(documentUtils).findDocumentIds(anyMap());
    }

    @Test
    void testShouldReturnAllDocumentHashesWhenAllPresentInSavedData() {
        // Given
        final List<DocumentHashToken> documentHashes = List.of(HASH_TOKEN_A1, HASH_TOKEN_A2);
        final Set<String> savedDocumentIds = Set.of(HASH_TOKEN_A1.getId(), HASH_TOKEN_A2.getId());

        doReturn(savedDocumentIds).when(documentUtils).findDocumentIds(anyMap());

        // When
        final List<DocumentHashToken> result = underTest.filterDocumentHashesAgainstSavedData(documentHashes, emptyMap());

        // Then
        assertThat(result)
            .hasSize(2)
            .hasSameElementsAs(List.of(HASH_TOKEN_A1, HASH_TOKEN_A2));
        verify(documentUtils).findDocumentIds(anyMap());
    }

    @Test
    void testShouldReturnEmptyListWhenNoDocumentHashesMatchSavedData() {
        // Given
        final List<DocumentHashToken> documentHashes = List.of(HASH_TOKEN_A1, HASH_TOKEN_A2);
        final Set<String> savedDocumentIds = Set.of(HASH_TOKEN_B1.getId(), HASH_TOKEN_B2.getId());

        doReturn(savedDocumentIds).when(documentUtils).findDocumentIds(anyMap());

        // When
        final List<DocumentHashToken> result = underTest.filterDocumentHashesAgainstSavedData(documentHashes, emptyMap());

        // Then
        assertThat(result).isEmpty();
        verify(documentUtils).findDocumentIds(anyMap());
    }

    @Test
    void testShouldNotCallFindDocumentIdsWhenDocumentHashesDroppedByCallback() {
        // Given - document was in event payload but callback dropped it
        final List<DocumentHashToken> documentHashes = List.of(HASH_TOKEN_A1);
        final Set<String> savedDocumentIds = emptySet();

        doReturn(savedDocumentIds).when(documentUtils).findDocumentIds(anyMap());

        // When
        final List<DocumentHashToken> result = underTest.filterDocumentHashesAgainstSavedData(documentHashes, emptyMap());

        // Then
        assertThat(result).isEmpty();
        verify(documentUtils).findDocumentIds(anyMap());
        verify(documentUtils, never()).findDocumentNodes(anyMap());
    }

    @Test
    void testShouldFilterDocumentHashesUsingRealDataTraversal() throws Exception {
        // Given
        final CaseDocumentUtils realDocumentUtils = new CaseDocumentUtils();
        final CaseDocumentService serviceWithRealUtils = new CaseDocumentService(
            caseService,
            realDocumentUtils,
            applicationParams,
            caseDocumentAmApiClient
        );

        final Map<String, JsonNode> savedData = fromFileAsMap("case-data.json");

        // All document IDs present in the JSON across all nested structures,
        // excluding hearingDoc as it contains hearing-recordings in the URL
        final String draftOrderDocId       = "f1f9a2c2-c309-4060-8f77-1800be0c85a8"; // A_Simple Document
        final String extraDoc1Id           = "7654b1e0-5df6-47c4-a5a0-3ec79fc78cfb"; // C_1_Collection
        final String extraDoc2Id           = "f6d623f2-db67-4a01-ae6e-3b6ee14a8b20"; // C_2_Collection
        final String timelineDoc1Id        = "f8e7f506-e8bd-4886-bd08-d4f70e9c84f6"; // D_1_Collection of Complex
        final String timelineDoc2Id        = "cdab9cf7-1b38-4245-9c91-e098d28f6404"; // D_2_Collection of Complex
        final String evidenceDocId         = "84f04693-56ae-4aad-97e8-d1fc7592acea"; // B_Document Inside Complex
        final String stateDoc1AId          = "5a16b8ed-c62f-41b3-b3c9-1df20b6a9979"; // E_1_AA
        final String stateDoc1BId          = "19de0db3-37c6-4191-a81d-c31a1379a9ca"; // E_1_BB
        final String stateDoc2AId          = "f51456a2-7b25-4855-844a-81c9763bc02c"; // E_2_AA
        final String stateDoc2BId          = "2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec"; // E_2_BB
        final String stateDoc3AId          = "c6924316-4146-441d-a66d-6e181c48cb09"; // E_3_AA
        final String stateDoc3BId          = "77ad6295-59ce-4167-8f1e-4aa711ba2c00"; // E_3_BB
        final String stateDoc3CId          = "80e9471e-0f67-42ef-8739-170aa1942363"; // E_3_CC
        final String missingDocId          = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"; // not in saved data

        // Build document hashes - all saved docs plus one that was dropped by callback
        final List<DocumentHashToken> documentHashes = List.of(
            DocumentHashToken.builder().id(draftOrderDocId).hashToken("hash1").build(),
            DocumentHashToken.builder().id(extraDoc1Id).hashToken("hash2").build(),
            DocumentHashToken.builder().id(extraDoc2Id).hashToken("hash3").build(),
            DocumentHashToken.builder().id(timelineDoc1Id).hashToken("hash4").build(),
            DocumentHashToken.builder().id(timelineDoc2Id).hashToken("hash5").build(),
            DocumentHashToken.builder().id(evidenceDocId).hashToken("hash6").build(),
            DocumentHashToken.builder().id(stateDoc1AId).hashToken("hash7").build(),
            DocumentHashToken.builder().id(stateDoc1BId).hashToken("hash8").build(),
            DocumentHashToken.builder().id(stateDoc2AId).hashToken("hash9").build(),
            DocumentHashToken.builder().id(stateDoc2BId).hashToken("hash10").build(),
            DocumentHashToken.builder().id(stateDoc3AId).hashToken("hash11").build(),
            DocumentHashToken.builder().id(stateDoc3BId).hashToken("hash12").build(),
            DocumentHashToken.builder().id(stateDoc3CId).hashToken("hash13").build(),
            DocumentHashToken.builder().id(missingDocId).hashToken("hash14").build()
        );

        // When
        final List<DocumentHashToken> result = serviceWithRealUtils
            .filterDocumentHashesAgainstSavedData(documentHashes, savedData);

        assertThat(result)
            .hasSize(13)
            .extracting(DocumentHashToken::getId)
            .containsExactlyInAnyOrder(
                draftOrderDocId,
                extraDoc1Id,
                extraDoc2Id,
                timelineDoc1Id,
                timelineDoc2Id,
                evidenceDocId,
                stateDoc1AId,
                stateDoc1BId,
                stateDoc2AId,
                stateDoc2BId,
                stateDoc3AId,
                stateDoc3BId,
                stateDoc3CId
            )
            .doesNotContain(missingDocId);
    }

    @Test
    void testShouldNotAttachHearingRecordingDocumentFromSavedData() throws Exception {
        // Given - hearingDoc in saved data should be excluded from traversal entirely
        final CaseDocumentUtils realDocumentUtils = new CaseDocumentUtils();
        final CaseDocumentService serviceWithRealUtils = new CaseDocumentService(
            caseService,
            realDocumentUtils,
            applicationParams,
            caseDocumentAmApiClient
        );

        final Map<String, JsonNode> savedData = fromFileAsMap("case-data.json");

        final String hearingDocId = "39a196bf-f105-4e47-840a-90f661b54ed0";

        final List<DocumentHashToken> documentHashes = List.of(
            DocumentHashToken.builder().id(hearingDocId).hashToken("hash1").build()
        );

        // When
        final List<DocumentHashToken> result = serviceWithRealUtils
            .filterDocumentHashesAgainstSavedData(documentHashes, savedData);

        assertThat(result).isEmpty();
    }
}
