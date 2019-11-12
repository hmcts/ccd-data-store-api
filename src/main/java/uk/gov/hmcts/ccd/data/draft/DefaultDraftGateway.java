package uk.gov.hmcts.ccd.data.draft;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.DraftResponseToCaseDetailsBuilder;
import uk.gov.hmcts.ccd.domain.model.draft.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

@Service
@Qualifier(DefaultDraftGateway.QUALIFIER)
public class DefaultDraftGateway implements DraftGateway {

    public static final String QUALIFIER = "default";
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDraftGateway.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DRAFT_ENCRYPTION_KEY_HEADER = "Secret";
    private static final int RESOURCE_NOT_FOUND = 404;
    public static final String DRAFT_STORE_DOWN_ERR_MESSAGE = "The draft service is currently down, please refresh your browser or try again later";
    private static final String RESOURCE_NOT_FOUND_MSG = "No draft found ( draft reference = '%s' )";
    private static final String DRAFT_STORE_DESERIALIZATION_ERR_MESSAGE = "Unable to read from draft service";

    private final RestTemplate createDraftRestTemplate;
    private final RestTemplate restTemplate;
    private final SecurityUtils securityUtils;
    private final ApplicationParams applicationParams;
    private final DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;

    @Inject
    public DefaultDraftGateway(
        @Qualifier("createDraftRestTemplate") final RestTemplate createDraftRestTemplate,
        @Qualifier("draftsRestTemplate") final RestTemplate restTemplate,
        final SecurityUtils securityUtils,
        final ApplicationParams applicationParams,
        final DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder) {
        this.createDraftRestTemplate = createDraftRestTemplate;
        this.restTemplate = restTemplate;
        this.securityUtils = securityUtils;
        this.applicationParams = applicationParams;
        this.draftResponseToCaseDetailsBuilder = draftResponseToCaseDetailsBuilder;
    }

    @Override
    public Long create(final CreateCaseDraftRequest draft) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(draft, headers);
            HttpHeaders responseHeaders = createDraftRestTemplate.exchange(applicationParams.draftBaseURL(),
                                                                           HttpMethod.POST,
                                                                           requestEntity,
                                                                           HttpEntity.class).getHeaders();
            return getDraftId(responseHeaders);
        } catch (Exception e) {
            LOG.warn("Error while saving draft", e);
            throw new ServiceException(DRAFT_STORE_DOWN_ERR_MESSAGE, e);
        }
    }

    @Override
    public DraftResponse update(final UpdateCaseDraftRequest draft, final String draftId) {
        HttpHeaders headers = securityUtils.authorizationHeaders();
        headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
        final HttpEntity requestEntity = new HttpEntity(draft, headers);
        try {
            restTemplate.exchange(applicationParams.draftURL(draftId), HttpMethod.PUT, requestEntity, HttpEntity.class);
        } catch (HttpClientErrorException e) {
            LOG.warn("Error while updating draftId={}", draftId, e);
            if (e.getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException(String.format(RESOURCE_NOT_FOUND_MSG, draftId));
            }
            throw new ApiException(DRAFT_STORE_DOWN_ERR_MESSAGE, e);
        } catch (Exception e) {
            LOG.warn("Error while updating draftId={}", draftId, e);
            throw new ServiceException(DRAFT_STORE_DOWN_ERR_MESSAGE, e);
        }
        final DraftResponse draftResponse = new DraftResponse();
        draftResponse.setId(draftId);
        return draftResponse;
    }

    @Override
    public DraftResponse get(final String draftId) {
        HttpHeaders headers = securityUtils.authorizationHeaders();
        headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
        final HttpEntity requestEntity = new HttpEntity(headers);
        Draft draft = null;
        try {
            draft = restTemplate.exchange(applicationParams.draftURL(draftId), HttpMethod.GET, requestEntity, Draft.class).getBody();
        } catch (HttpClientErrorException e) {
            LOG.warn("Error while getting draftId={}", draftId, e);
            if (e.getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException(String.format(RESOURCE_NOT_FOUND_MSG, draftId));
            }
            throw new ApiException(DRAFT_STORE_DOWN_ERR_MESSAGE, e);
        } catch (Exception e) {
            LOG.warn("Error while getting draftId={}", draftId, e);
            throw new ServiceException(DRAFT_STORE_DOWN_ERR_MESSAGE, e);
        }
        return assembleDraft(draft, getDraftExceptionConsumer());
    }

    @Override
    public CaseDetails getCaseDetails(String draftId) {
        DraftResponse draftResponse = get(draftId);
        return draftResponseToCaseDetailsBuilder.build(draftResponse);
    }

    @Override
    public void delete(final String draftId) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(headers);
            restTemplate.exchange(applicationParams.draftURL(draftId), HttpMethod.DELETE, requestEntity, Draft.class);
        } catch (HttpClientErrorException e) {
            LOG.warn("Error while deleting draftId=" + draftId, e);
            if (e.getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException(String.format(RESOURCE_NOT_FOUND_MSG, draftId));
            }
            throw new ApiException(DRAFT_STORE_DOWN_ERR_MESSAGE, e);
        } catch (Exception e) {
            LOG.warn("Error while deleting draftId=" + draftId, e);
            throw new ServiceException(DRAFT_STORE_DOWN_ERR_MESSAGE, e);
        }
    }

    @Override
    public List<DraftResponse> getAll() {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(headers);
            DraftList getDrafts = restTemplate.exchange(getUriWithQueryParams(), HttpMethod.GET, requestEntity, DraftList.class).getBody();
            return getDrafts.getData()
                .stream()
                .map(d -> assembleDraft(d, getDraftsExceptionConsumer()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.warn("Error while getting drafts", e);
            throw new DraftAccessException(DRAFT_STORE_DOWN_ERR_MESSAGE, e);
        }
    }

    private Consumer<Exception> getDraftExceptionConsumer() {
        return (Exception e) -> {
            LOG.warn("Error while deserializing draft data content", e);
            throw new ServiceException(DRAFT_STORE_DESERIALIZATION_ERR_MESSAGE, e);
        };
    }

    private Consumer<Exception> getDraftsExceptionConsumer() {
        return (Exception e) -> {
            LOG.warn("Error while deserializing draft data content", e);
        };
    }

    private String getUriWithQueryParams() {
        return UriComponentsBuilder.fromUriString(applicationParams.draftBaseURL())
            .queryParam("limit", Integer.MAX_VALUE).toUriString();
    }

    private DraftResponse assembleDraft(Draft getDraft, Consumer<Exception> exceptionConsumer) {
        final DraftResponse draftResponse = new DraftResponse();
        try {
            draftResponse.setId(getDraft.getId());
            draftResponse.setDocument(MAPPER.treeToValue(getDraft.getDocument(), CaseDraft.class));
            draftResponse.setType(getDraft.getType());
            draftResponse.setCreated(getDraft.getCreated().toLocalDateTime());
            draftResponse.setUpdated(getDraft.getUpdated().toLocalDateTime());
        } catch (IOException e) {
            exceptionConsumer.accept(e);
        }
        return draftResponse;
    }


    private Long getDraftId(HttpHeaders responseHeaders) {
        String path = responseHeaders.getLocation().getPath();
        return Long.valueOf(path.substring(path.lastIndexOf('/') + 1));
    }
}
