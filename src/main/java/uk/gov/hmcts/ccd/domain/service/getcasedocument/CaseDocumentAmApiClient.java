package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.endpoint.exceptions.DocumentTokenException;

public class CaseDocumentAmApiClient {
    private final RestTemplate restTemplate;
    private final SecurityUtils securityUtils;
    private final ApplicationParams applicationParams;

    public CaseDocumentAmApiClient(final RestTemplate restTemplate,
                                   final SecurityUtils securityUtils,
                                   final ApplicationParams applicationParams) {
        this.restTemplate = restTemplate;
        this.securityUtils = securityUtils;
        this.applicationParams = applicationParams;
    }

    public void attachCaseDocuments(final CaseDocumentsMetadata caseDocumentsMetadata) {
        final HttpHeaders headers = securityUtils.authorizationHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        final HttpEntity<CaseDocumentsMetadata> requestEntity = new HttpEntity<>(caseDocumentsMetadata, headers);

        try {
            restTemplate.exchange(applicationParams.getCaseDocumentAmApiHost()
                    .concat(applicationParams.getAttachDocumentPath()),
                HttpMethod.PATCH, requestEntity, Void.class);

        } catch (HttpClientErrorException restClientException) {
//            if (restClientException.getStatusCode() != HttpStatus.FORBIDDEN) {
//                exceptionScenarios(restClientException);
//            }
//            final String badDocument = restClientException.getResponseBodyAsString();
//
//            throw new DocumentTokenException(
//                String.format("The user has provided an invalid hashToken for document %s", badDocument));
        }
    }

}
