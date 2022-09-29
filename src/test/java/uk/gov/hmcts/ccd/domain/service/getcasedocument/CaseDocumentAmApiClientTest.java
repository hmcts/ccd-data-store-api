package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import feign.FeignException;
import feign.FeignException.FeignClientException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.DocumentTokenException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.PatchDocumentMetaDataResponse;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CaseDocumentAmApiClientTest extends TestFixtures {
    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private CaseDocumentMetadataMapper caseDocumentMetadataMapper;

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;

    @InjectMocks
    private CaseDocumentAmApiClient underTest;

    private final CaseDocumentsMetadata documentMetadata = buildCaseDocumentsMetadata();

    @BeforeEach
    void prepare() {
        doReturn("authValue").when(securityUtils).getUserBearerToken();
        doReturn("serviceAuthValue").when(securityUtils).getServiceAuthorization();
    }

    @Test
    void testShouldReturnSuccess() {
        // GIVEN
        doReturn(uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata.builder().build())
            .when(caseDocumentMetadataMapper).convertToAmClientCaseDocumentsMetadata(any(CaseDocumentsMetadata.class));

        PatchDocumentMetaDataResponse patchDocumentMetaDataResponse = new PatchDocumentMetaDataResponse("Success");
        doReturn(patchDocumentMetaDataResponse).when(caseDocumentClientApi).patchDocument(anyString(), anyString(),
            any(uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata.class));

        // WHEN
        underTest.applyPatch(documentMetadata);

        // THEN
        verify(caseDocumentClientApi).patchDocument(
            anyString(),
            anyString(),
            any(uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata.class)
        );
    }

    @ParameterizedTest
    @MethodSource("provideErrorScenarioParameters")
    void testShouldRaiseException(final FeignException feignException, final String errorMessage, final Class<?> type) {
        // GIVEN
        doReturn(uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata.builder().build())
            .when(caseDocumentMetadataMapper).convertToAmClientCaseDocumentsMetadata(any(CaseDocumentsMetadata.class));
        doThrow(feignException).when(caseDocumentClientApi).patchDocument(anyString(),
            anyString(),
            any(uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata.class)
        );

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.applyPatch(documentMetadata));

        // THEN
        assertThat(thrown)
            .isInstanceOf(type)
            .hasMessageContaining(errorMessage);

        verify(caseDocumentClientApi).patchDocument(anyString(),anyString(),
            any(uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata.class));
    }

    private CaseDocumentsMetadata buildCaseDocumentsMetadata() {
        return CaseDocumentsMetadata.builder()
            .caseId(CASE_REFERENCE)
            .caseTypeId("Some-Case-ID")
            .jurisdictionId(JURISDICTION_ID)
            .documentHashTokens(emptyList())
            .build();
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideErrorScenarioParameters() {
        final String badRequestMessage = "The input parameter does not comply with the required pattern";
        final String notFoundMessage = "The resource X was not found";
        final String serviceErrorMessage = "The downstream CCD CDAM application has failed";
        final String forbiddenMessage = "The user has provided an invalid hashToken for document";

        Request request = Request.create(Request.HttpMethod.GET, "someUrl", Map.of(),
            null, Charset.defaultCharset(),
            null
        );

        return Stream.of(
            Arguments.of(new FeignClientException.BadRequest(badRequestMessage, request, null,
                new HashMap<String, Collection<String>>()),
                badRequestMessage,
                BadSearchRequest.class),
            Arguments.of(new FeignClientException.NotFound(notFoundMessage, request, null,
                new HashMap<String, Collection<String>>()),
                notFoundMessage,
                ResourceNotFoundException.class),
            Arguments.of(new FeignClientException.InternalServerError(serviceErrorMessage, request, null,
                new HashMap<String, Collection<String>>()),
                serviceErrorMessage,
                ServiceException.class),
            Arguments.of(new FeignClientException.Forbidden(forbiddenMessage, request, null,
                new HashMap<String, Collection<String>>()),
                forbiddenMessage,
                DocumentTokenException.class)
        );
    }
}
