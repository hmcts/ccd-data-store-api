package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.DocumentTokenException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

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
    private RestTemplate restTemplate;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private CaseDocumentAmApiClient underTest;

    private final CaseDocumentsMetadata documentMetadata = buildCaseDocumentsMetadata();

    @BeforeEach
    void prepare() {
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn("http://localhost:4455").when(applicationParams).getCaseDocumentAmApiHost();
        doReturn("/cases/documents/attachToCase").when(applicationParams).getAttachDocumentPath();
    }

    @Test
    void testShouldReturnSuccess() {
        // GIVEN
        final ResponseEntity<String> responseEntity = new ResponseEntity<>("Success", HttpStatus.OK);
        doReturn(responseEntity).when(restTemplate).exchange(
            anyString(),
            any(HttpMethod.class),
            ArgumentMatchers.<HttpEntity<CaseDocumentsMetadata>>any(),
            ArgumentMatchers.<Class<String>>any()
        );

        // WHEN
        underTest.applyPatch(documentMetadata);

        // THEN
        verify(securityUtils).authorizationHeaders();
        verify(applicationParams).getCaseDocumentAmApiHost();
        verify(applicationParams).getAttachDocumentPath();
        verify(restTemplate).exchange(
            anyString(),
            any(HttpMethod.class),
            ArgumentMatchers.<HttpEntity<CaseDocumentsMetadata>>any(),
            ArgumentMatchers.<Class<String>>any()
        );
    }

    @ParameterizedTest
    @MethodSource("provideErrorScenarioParameters")
    void testShouldRaiseException(final HttpStatus status, final String errorMessage, final Class<?> type) {
        // GIVEN
        final HttpClientErrorException httpClientErrorException = new HttpClientErrorException(
            status,
            errorMessage
        );
        doThrow(httpClientErrorException).when(restTemplate).exchange(
            anyString(),
            any(HttpMethod.class),
            ArgumentMatchers.<HttpEntity<CaseDocumentsMetadata>>any(),
            ArgumentMatchers.<Class<String>>any()
        );

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.applyPatch(documentMetadata));

        // THEN
        assertThat(thrown)
            .isInstanceOf(type)
            .hasMessageContaining(errorMessage);

        verify(securityUtils).authorizationHeaders();
        verify(applicationParams).getCaseDocumentAmApiHost();
        verify(applicationParams).getAttachDocumentPath();
        verify(restTemplate).exchange(
            anyString(),
            any(HttpMethod.class),
            ArgumentMatchers.<HttpEntity<CaseDocumentsMetadata>>any(),
            ArgumentMatchers.<Class<String>>any()
        );
    }

    private CaseDocumentsMetadata buildCaseDocumentsMetadata() {
        return CaseDocumentsMetadata.builder()
            .caseId(CASE_REFERENCE)
            .caseTypeId("Some-Case-ID")
            .jurisdictionId(JURISDICTION)
            .documentHashToken(emptyList())
            .build();
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideErrorScenarioParameters() {
        final String badRequestMessage = "The input parameter does not comply with the required pattern";
        final String notFoundMessage = "The resource X was not found";
        final String serviceErrorMessage = "The downstream CCD AM application has failed";
        final String forbiddenMessage = "The user has provided an invalid hashToken for document";
        return Stream.of(
            Arguments.of(HttpStatus.BAD_REQUEST, badRequestMessage, BadSearchRequest.class),
            Arguments.of(HttpStatus.NOT_FOUND, notFoundMessage, ResourceNotFoundException.class),
            Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, serviceErrorMessage, ServiceException.class),
            Arguments.of(HttpStatus.FORBIDDEN, forbiddenMessage, DocumentTokenException.class)
        );
    }
}
