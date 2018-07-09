package uk.gov.hmcts.ccd.data.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.draft.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.model.draft.DraftBuilder.aDraft;


@Named
@Qualifier(DefaultDraftGateway.QUALIFIER)
@Singleton
public class DefaultDraftGateway implements DraftGateway {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDraftGateway.class);
    protected static final ObjectMapper mapper = new ObjectMapper();
    public static final String QUALIFIER = "default";
    private static final String DRAFT_ENCRYPTION_KEY_HEADER = "Secret";
    private static final int RESOURCE_NOT_FOUND = 404;

    private final RestTemplate restTemplate;
    private final SecurityUtils securityUtils;
    private final ApplicationParams applicationParams;

    @Inject
    public DefaultDraftGateway(
            final RestTemplate restTemplate,
            final SecurityUtils securityUtils,
            final ApplicationParams applicationParams) {
        this.restTemplate = restTemplate;
        this.securityUtils = securityUtils;
        this.applicationParams = applicationParams;
    }

    @Override
    public Long save(final CreateCaseDraft draft) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(draft, headers);
            HttpHeaders responseHeaders = restTemplate.exchange(applicationParams.draftBaseURL(), HttpMethod.POST, requestEntity, HttpEntity.class).getHeaders();
            return getDraftId(responseHeaders);
        } catch (Exception e) {
            LOG.warn("Error while saving draft=" + draft, e);
            throw new ServiceException("Problem saving draft because of " + e.getMessage());
        }
    }

    @Override
    public Draft update(final UpdateCaseDraft draft, final String draftId) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(draft, headers);
            restTemplate.exchange(applicationParams.draftURL(draftId), HttpMethod.PUT, requestEntity, HttpEntity.class);
            return aDraft()
                .withId(draftId)
                .build();
        } catch (HttpClientErrorException e) {
            LOG.warn("Error while updating draftId=" + draftId, e);
            if (e.getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException("Resource not found when getting draft for draftId=" + draftId + " because of " + e.getMessage());
            }
        } catch (Exception e) {
            LOG.warn("Error while updating draftId=" + draftId, e);
            throw new ServiceException("Problem updating draft because of " + e.getMessage());
        }
        return null;
    }

    @Override
    public Draft get(final String draftId) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(headers);
            GetDraft getDraft = restTemplate.exchange(applicationParams.draftURL(draftId), HttpMethod.GET, requestEntity, GetDraft.class).getBody();
            return assembleDraft(getDraft);
        } catch (HttpClientErrorException e) {
            LOG.warn("Error while getting draftId=" + draftId, e);
            if (e.getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException("Resource not found when getting draft for draftId=" + draftId + " because of " + e.getMessage());
            }
        } catch (Exception e) {
            LOG.warn("Error while getting draftId=" + draftId, e);
            throw new ServiceException("Problem getting draft because of " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Draft> getAll() {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(headers);
            DraftList getDrafts = restTemplate.exchange(applicationParams.draftBaseURL(), HttpMethod.GET, requestEntity, DraftList.class).getBody();
            return getDrafts.getData();
        } catch (Exception e) {
            LOG.warn("Error while getting drafts", e);
            throw new ServiceException("Problem getting drafts because of " + e.getMessage());
        }
    }

    private Draft assembleDraft(GetDraft getDraft) {
        Draft draft = null;
        try {
            draft = aDraft()
                .withId(getDraft.getId())
                .withDocument(mapper.treeToValue(getDraft.getDocument(), CaseDraft.class))
                .withType(getDraft.getType())
                .withCreated(getDraft.getCreated().toLocalDateTime())
                .withUpdated(getDraft.getUpdated().toLocalDateTime())
                .build();
        } catch (IOException e) {
            LOG.warn("Error while deserializing case data content", e);
            throw new ServiceException("Problem deserializing case data content because of " + e.getMessage());
        }
        return draft;
    }

    private Long getDraftId(HttpHeaders responseHeaders) {
        String path = responseHeaders.getLocation().getPath();
        return Long.valueOf(path.substring(path.lastIndexOf("/") + 1));
    }
}
