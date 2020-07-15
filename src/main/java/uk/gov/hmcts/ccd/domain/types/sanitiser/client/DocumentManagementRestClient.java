package uk.gov.hmcts.ccd.domain.types.sanitiser.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document.Document;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class DocumentManagementRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentManagementRestClient.class);

    private final SecurityUtils securityUtils;
    @Qualifier("documentRestTemplate")
    @Autowired
    private final RestTemplate restTemplate;

    public DocumentManagementRestClient(final SecurityUtils securityUtils,
                                        final RestTemplate restTemplate) {
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
    }

    public Document getDocument(FieldTypeDefinition fieldTypeDefinition, String url) {
        final HttpHeaders headers = securityUtils.authorizationHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity requestEntity = new HttpEntity(headers);

        Document document = null;
        try {
            LOG.info("Requesting from Document management: {}", url);
            document = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Document.class).getBody();

        } catch (Exception e) {
            LOG.error("Cannot sanitize document for the Case Field Type:{}, Case Field Type Id:{} because of unreachable url",
                fieldTypeDefinition.getType(), fieldTypeDefinition.getId(), e);
            throw new ApiException(String.format("Cannot sanitize document for the Case Field Type:%s, Case Field Type Id:%s because of %s",
                fieldTypeDefinition.getType(), fieldTypeDefinition.getId(), e));
        }

        return document;
    }
}
