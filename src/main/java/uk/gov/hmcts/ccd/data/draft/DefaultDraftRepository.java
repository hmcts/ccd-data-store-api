package uk.gov.hmcts.ccd.data.draft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;


@Named
@Qualifier(DefaultDraftRepository.QUALIFIER)
@Singleton
public class DefaultDraftRepository implements DraftRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDraftRepository.class);
    public static final String QUALIFIER = "default";
    private static final String DRAFT_ENCRYPTION_KEY_HEADER = "Secret";

    private final RestTemplate restTemplate;
    private final SecurityUtils securityUtils;
    private final ApplicationParams applicationParams;

    @Inject
    public DefaultDraftRepository(
            final RestTemplate restTemplate,
            final SecurityUtils securityUtils,
            final ApplicationParams applicationParams) {
        this.restTemplate = restTemplate;
        this.securityUtils = securityUtils;
        this.applicationParams = applicationParams;
    }

    @Override
    public Draft set(final CreateCaseDataContentDraft draft) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(draft, headers);
            HttpHeaders responseHeaders = restTemplate.exchange(applicationParams.draftBaseURL(), HttpMethod.POST, requestEntity, HttpEntity.class).getHeaders();
            return assembleDraft(draft, responseHeaders);
        } catch (Exception e) {
            LOG.warn("Error while saving draft=" + draft, e);
            throw new ServiceException("Problem saving draft=" + draft + " because of " + e.getMessage());
        }

    }

    private Draft assembleDraft(CreateCaseDataContentDraft requestDraft, HttpHeaders responseHeaders) {
        Draft responseDraft = new Draft();
        responseDraft.setId(getId(responseHeaders));
        return responseDraft;
    }

    private Long getId(HttpHeaders responseHeaders) {
        return Long.valueOf(responseHeaders.getLocation().getRawPath().split("/")[2]);
    }
}
