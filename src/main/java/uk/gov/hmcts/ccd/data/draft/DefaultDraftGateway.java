package uk.gov.hmcts.ccd.data.draft;

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
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDataContentDraft;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;


@Named
@Qualifier(DefaultDraftGateway.QUALIFIER)
@Singleton
public class DefaultDraftGateway implements DraftGateway {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDraftGateway.class);
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
    public Draft save(final CreateCaseDataContentDraft draft) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(draft, headers);
            HttpHeaders responseHeaders = restTemplate.exchange(applicationParams.draftBaseURL(), HttpMethod.POST, requestEntity, HttpEntity.class).getHeaders();
            return assembleDraft(responseHeaders);
        } catch (Exception e) {
            LOG.warn("Error while saving draft=" + draft, e);
            throw new ServiceException("Problem saving draft because of " + e.getMessage());
        }
    }

    @Override
    public Draft update(final UpdateCaseDataContentDraft draft, final String draftId) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(draft, headers);
            restTemplate.exchange(applicationParams.draftURL(draftId), HttpMethod.PUT, requestEntity, HttpEntity.class);
            return assembleDraft(Long.valueOf(draftId));
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

    private Draft assembleDraft(HttpHeaders responseHeaders) {
        Draft responseDraft = new Draft();
        responseDraft.setId(getDraftId(responseHeaders));
        return responseDraft;
    }

    private Draft assembleDraft(Long draftId) {
        Draft draft = new Draft();
        draft.setId(draftId);
        return draft;
    }

    private Long getDraftId(HttpHeaders responseHeaders) {
        String path = responseHeaders.getLocation().getPath();
        return Long.valueOf(path.substring(path.lastIndexOf("/") + 1));
    }
}
