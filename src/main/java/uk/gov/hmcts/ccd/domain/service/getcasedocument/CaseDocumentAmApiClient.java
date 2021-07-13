package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import feign.FeignException;
import feign.FeignException.FeignClientException;
import org.springframework.http.HttpHeaders;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.DocumentTokenException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;

import javax.inject.Inject;
import javax.inject.Named;

import static uk.gov.hmcts.ccd.data.SecurityUtils.SERVICE_AUTHORIZATION;

@Named
public class CaseDocumentAmApiClient {
    private final CaseDocumentClientApi caseDocumentClientApi;
    private final SecurityUtils securityUtils;
    private final CaseDocumentMetadataMapper caseDocumentMetadataMapper;

    @Inject
    public CaseDocumentAmApiClient(final CaseDocumentClientApi caseDocumentClientApi,
                                   final SecurityUtils securityUtils,
                                   final CaseDocumentMetadataMapper caseDocumentMetadataMapper) {
        this.caseDocumentClientApi = caseDocumentClientApi;
        this.securityUtils = securityUtils;
        this.caseDocumentMetadataMapper = caseDocumentMetadataMapper;
    }

    public void applyPatch(final CaseDocumentsMetadata caseDocumentsMetadata) {
        final HttpHeaders headers = securityUtils.authorizationHeaders();

        uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata mappedCaseDocumentsMetadata =
            caseDocumentMetadataMapper.convertToAmClientCaseDocumentsMetadata(caseDocumentsMetadata);

        String authorization = headers.get(HttpHeaders.AUTHORIZATION).get(0);
        String serviceAuthorization = headers.get(SERVICE_AUTHORIZATION).get(0);
        try {
            caseDocumentClientApi.patchDocument(authorization, serviceAuthorization, mappedCaseDocumentsMetadata);
        } catch (FeignException feignException) {
            if (!(feignException instanceof FeignClientException.Forbidden)) {
                exceptionScenarios(feignException);
            }
            final String badDocument = feignException.getMessage();

            throw new DocumentTokenException(
                String.format("The user has provided an invalid hashToken for document %s", badDocument)
            );
        }
    }

    private void exceptionScenarios(FeignException feignException) {
        if (feignException instanceof FeignClientException.BadRequest) {
            throw new BadSearchRequest(feignException.getMessage());
        } else if (feignException instanceof FeignClientException.NotFound) {
            throw new ResourceNotFoundException(feignException.getMessage());
        } else {
            throw new ServiceException("The downstream CCD AM application has failed");
        }
    }

}
