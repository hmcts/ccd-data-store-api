package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CaseDocumentServiceTest extends TestFixtures {
    private static final String JURISDICTION = "SSCS";
    private static final String CASE_REFERENCE = "1234123412341236";
    private static final Long REFERENCE = Long.valueOf(CASE_REFERENCE);
    private static final String STATE = "CreatedState";

    @Mock
    private CaseService caseService;

    @Mock
    private CaseDocumentUtils documentUtils;

    @Mock
    private CaseDocumentAmApiClient caseDocumentAmApiClient;

    @InjectMocks
    private CaseDocumentService underTest;

    @Test
    void testShouldReturnClonedCaseDetailsWithoutHashes() throws Exception {
        // Given
        final Map<String, JsonNode> data = loadDataAsMap("new-document-with-hashtoken.json");
        final CaseDetails caseDetails = buildCaseDetails(data);

        final Map<String, JsonNode> dataWithoutHashes = loadDataAsMap("new-document-with-removed-hashtoken.json");

        doReturn(caseDetails).when(caseService).clone(caseDetails);

        // When
        final CaseDetails actualClonedCaseDetails = underTest.cloneCaseDetailsWithoutHashes(caseDetails);

        // Then
        verify(caseService).clone(caseDetails);

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
    }

    @Test
    void test() {
        // When
        underTest.attachCaseDocuments(null, null);
    }

    private CaseDetails buildCaseDetails(final Map<String, JsonNode> data) {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setReference(REFERENCE);
        caseDetails.setState(STATE);

        caseDetails.setData(data);

        return caseDetails;
    }
}
