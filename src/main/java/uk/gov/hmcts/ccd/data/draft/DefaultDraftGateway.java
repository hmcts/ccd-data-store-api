package uk.gov.hmcts.ccd.data.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.draft.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.AppInsights.DOC_MANAGEMENT;
import static uk.gov.hmcts.ccd.AppInsights.DRAFT_STORE;
import static uk.gov.hmcts.ccd.domain.model.draft.DraftResponseBuilder.aDraftResponse;


@Service
@Qualifier(DefaultDraftGateway.QUALIFIER)
public class DefaultDraftGateway implements DraftGateway {

    public static final String QUALIFIER = "default";
    public static final String DRAFT_ACCESS_EXCEPTION_MSG = "There is a problem with retrieving drafts.";
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDraftGateway.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DRAFT_ENCRYPTION_KEY_HEADER = "Secret";
    private static final int RESOURCE_NOT_FOUND = 404;

    @Qualifier("draftsRestTemplate")
    @Autowired
    private final RestTemplate restTemplate;
    private final SecurityUtils securityUtils;
    private final ApplicationParams applicationParams;
    private final AppInsights appInsights;

    @Inject
    public DefaultDraftGateway(
        final RestTemplate restTemplate,
        final SecurityUtils securityUtils,
        final ApplicationParams applicationParams,
        final AppInsights appInsights) {
        this.restTemplate = restTemplate;
        this.securityUtils = securityUtils;
        this.applicationParams = applicationParams;
        this.appInsights = appInsights;
    }

    @Override
    public Long save(final CreateCaseDraft draft) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(draft, headers);
            final Instant start = Instant.now();
            HttpHeaders responseHeaders = restTemplate.exchange(applicationParams.draftBaseURL(),
                                                                HttpMethod.POST,
                                                                requestEntity,
                                                                HttpEntity.class).getHeaders();
            final Duration duration = Duration.between(start, Instant.now());
            appInsights.trackDependency(DRAFT_STORE, "Create", duration.toMillis(), true);
            return getDraftId(responseHeaders);
        } catch (Exception e) {
            LOG.warn("Error while saving draft=" + draft, e);
            throw new ServiceException("Problem saving draft because of " + e.getMessage());
        }
    }

    @Override
    public DraftResponse update(final UpdateCaseDraft draft, final String draftId) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(draft, headers);
            final Instant start = Instant.now();
            restTemplate.exchange(applicationParams.draftURL(draftId), HttpMethod.PUT, requestEntity, HttpEntity.class);
            final Duration duration = Duration.between(start, Instant.now());
            appInsights.trackDependency(DRAFT_STORE, "Update", duration.toMillis(), true);
            return aDraftResponse()
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
    public DraftResponse get(final String draftId) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(headers);
            final Instant start = Instant.now();
            Draft draft = restTemplate.exchange(applicationParams.draftURL(draftId), HttpMethod.GET, requestEntity, Draft.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            appInsights.trackDependency(DRAFT_STORE, "Get", duration.toMillis(), true);
            return assembleDraft(draft);
        } catch (HttpClientErrorException e) {
            LOG.warn("Error while getting draftId=" + draftId, e);
            if (e.getRawStatusCode() == RESOURCE_NOT_FOUND) {
                throw new ResourceNotFoundException("Resource not found when getting draft for draftId=" + draftId);
            }
        } catch (Exception e) {
            LOG.warn("Error while getting draftId=" + draftId, e);
            throw new ServiceException("Problem getting draft because of " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<DraftResponse> getAll() {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            headers.add(DRAFT_ENCRYPTION_KEY_HEADER, applicationParams.getDraftEncryptionKey());
            final HttpEntity requestEntity = new HttpEntity(headers);
            final Instant start = Instant.now();
            DraftList getDrafts = restTemplate.exchange(getUriWithQueryParams(), HttpMethod.GET, requestEntity, DraftList.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            appInsights.trackDependency(DRAFT_STORE, "GetAll", duration.toMillis(), true);
            return getDrafts.getData()
                .stream()
                .map(this::assembleDraft)
                .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.warn("Error while getting drafts", e);
            throw new DraftAccessException(DRAFT_ACCESS_EXCEPTION_MSG);
        }
    }

    private String getUriWithQueryParams() {
        return UriComponentsBuilder.fromUriString(applicationParams.draftBaseURL())
            .queryParam("limit", Integer.MAX_VALUE).toUriString();
    }

    private DraftResponse assembleDraft(Draft getDraft) {
        DraftResponse draftResponse;
        try {
            draftResponse = aDraftResponse()
                .withId(getDraft.getId())
                .withDocument(MAPPER.treeToValue(getDraft.getDocument(), CaseDraft.class))
                .withType(getDraft.getType())
                .withCreated(getDraft.getCreated().toLocalDateTime())
                .withUpdated(getDraft.getUpdated().toLocalDateTime())
                .build();
        } catch (IOException e) {
            LOG.warn("Error while deserializing case data content", e);
            throw new ServiceException("Problem deserializing case data content because of " + e.getMessage());
        }
        return draftResponse;
    }

    private Long getDraftId(HttpHeaders responseHeaders) {
        String path = responseHeaders.getLocation().getPath();
        return Long.valueOf(path.substring(path.lastIndexOf("/") + 1));
    }
}
