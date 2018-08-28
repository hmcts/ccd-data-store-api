package uk.gov.hmcts.ccd.domain.types.sanitiser.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.types.sanitiser.document.Document;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;

import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static uk.gov.hmcts.ccd.AppInsights.DOC_MANAGEMENT;

@Named
@Singleton
public class DocumentManagementRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentManagementRestClient.class);

    private final SecurityUtils securityUtils;
    @Qualifier("restTemplate")
    @Autowired
    private final RestTemplate restTemplate;
    private final AppInsights appInsights;

    public DocumentManagementRestClient(final SecurityUtils securityUtils,
                                        final RestTemplate restTemplate,
                                        final AppInsights appInsights) {
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
        this.appInsights = appInsights;
    }

    public Document getDocument(FieldType fieldType, String url) {
        final HttpHeaders headers = securityUtils.authorizationHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity requestEntity = new HttpEntity(headers);

        // Need to ignore the response Content Type header from Document Management because it is not the expected type
        // of "application/json". See https://stackoverflow.com/a/44219832 for the same solution to a similar problem.
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        //Add the Jackson Message converter
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        // Note: here we are making this converter to process any kind of response,
        // not only application/*json, which is the default behaviour
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
        messageConverters.add(converter);
        restTemplate.setMessageConverters(messageConverters);

        Document document = null;
        try {
            final Instant start = Instant.now();
            LOG.info("Requesting from Document management: {}", url);
            document = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Document.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            appInsights.trackDependency(DOC_MANAGEMENT, "WorkbasketResult", duration.toMillis(), true);

        } catch (Exception e) {
            LOG.error("Cannot sanitize document for the Case Field Type:{}, Case Field Type Id:{} because of unreachable url",
                fieldType.getType(), fieldType.getId(), e);
            throw new ApiException(String.format("Cannot sanitize document for the Case Field Type:%s, Case Field Type Id:%s because of %s",
                fieldType.getType(), fieldType.getId(), e));
        }

        return document;
    }
}
