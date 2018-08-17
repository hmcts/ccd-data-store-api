package uk.gov.hmcts.ccd.data.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static uk.gov.hmcts.ccd.AppInsights.CASE_DEFINITION;

/**
 * NOTE: We want to cache definitions, so the only client of this class should be CachedUIDefinitionGateway
 */
@Named
@Qualifier("Http")
@Singleton
public class HttpUIDefinitionGateway implements UIDefinitionGateway {

    private static final Logger LOG = LoggerFactory.getLogger(UIDefinitionRepository.class);

    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;
    @Qualifier("restTemplate")
    @Autowired
    private final RestTemplate restTemplate;
    private final AppInsights appInsights;

    @Inject
    HttpUIDefinitionGateway(final ApplicationParams applicationParams,
                            final SecurityUtils securityUtils,
                            final RestTemplate restTemplate,
                            final AppInsights appInsights) {
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
        this.appInsights = appInsights;
    }

    @Override
    public SearchResult getSearchResult(int version, String caseTypeId) {
        try {
            final Instant start = Instant.now();
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            final SearchResult
                    searchResult =
                    restTemplate.exchange(withVersionQueryParam(applicationParams.displaySearchResultDefURL(caseTypeId), version),
                            HttpMethod.GET,
                            requestEntity,
                            SearchResult.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            LOG.debug("Rest API getSearchResultGetHttp called for {}, finished in {}",
                    caseTypeId,
                    duration.toMillis());
            appInsights.trackDependency(CASE_DEFINITION, "SearchResult", duration.toMillis(), true);
            return searchResult;
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                    "Problem getting SearchResult definition for case type: %s because of %s",
                    caseTypeId,
                    e.getMessage()));
        }
    }

    @Override
    public SearchInputDefinition getSearchInputDefinitions(int version, String caseTypeId) {
        try {
            final Instant start = Instant.now();
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            final SearchInputDefinition
                    definition =
                    restTemplate.exchange(withVersionQueryParam(applicationParams.searchInputDefinition(caseTypeId), version),
                            HttpMethod.GET,
                            requestEntity,
                            SearchInputDefinition.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            LOG.debug("Rest API getSearchInputDefinitionsGetHttp called for {}, finished in {}",
                    caseTypeId,
                    duration.toMillis());
            appInsights.trackDependency(CASE_DEFINITION, "SearchInputDefinitions", duration.toMillis(), true);
            return definition;
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                    "Problem getting SearchInputs definition for case type: %s because of %s",
                    caseTypeId,
                    e.getMessage()));
        }
    }

    @Override
    public WorkbasketInputDefinition getWorkbasketInputDefinitions(int version, String caseTypeId) {
        try {
            final Instant start = Instant.now();
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            final WorkbasketInputDefinition
                    definition =
                    restTemplate.exchange(withVersionQueryParam(applicationParams.workbasketInputDefinition(caseTypeId), version),
                            HttpMethod.GET,
                            requestEntity,
                            WorkbasketInputDefinition.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            LOG.debug("Rest API getWorkbasketInputDefinitionsGetHttp called for {}, finished in {}",
                    caseTypeId,
                    duration.toMillis());
            appInsights.trackDependency(CASE_DEFINITION, "WorkbasketInputDefinitions", duration.toMillis(), true);
            return definition;
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                    "Problem getting WorkbasketInputs definition for case type: %s because of %s",
                    caseTypeId,
                    e.getMessage()));
        }
    }

    @Override
    public CaseTabCollection getCaseTabCollection(int version, String caseTypeId) {
        final Instant start = Instant.now();
        final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
        final CaseTabCollection
                collection =
                restTemplate.exchange(withVersionQueryParam(applicationParams.displayCaseTabCollection(caseTypeId), version),
                        HttpMethod.GET,
                        requestEntity,
                        CaseTabCollection.class).getBody();
        final Duration duration = Duration.between(start, Instant.now());
        LOG.debug("Rest API getCaseTabCollectionGetHttp called for {}, finished in {}",
                caseTypeId,
                duration.toMillis());
        appInsights.trackDependency(CASE_DEFINITION, "CaseTabCollection", duration.toMillis(), true);
        return collection;
    }

    @Override
    public List<WizardPage> getWizardPageCollection(int version, String caseTypeId, String eventTriggerId) {
        final Instant start = Instant.now();
        final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
        final WizardPageCollection
                wpc =
                restTemplate.exchange(withVersionQueryParam(applicationParams.displayWizardPageCollection(caseTypeId, eventTriggerId), version),
                        HttpMethod.GET,
                        requestEntity,
                        WizardPageCollection.class).getBody();
        final Duration duration = Duration.between(start, Instant.now());
        LOG.debug("Rest API getWizardPageCollectionGetHttp called for {}, finished in {}",
                caseTypeId,
                duration.toMillis());
        appInsights.trackDependency(CASE_DEFINITION, "WizardPageCollection", duration.toMillis(), true);
        return wpc.getWizardPages();
    }

    @Override
    public SearchResult getWorkBasketResult(int version, String caseTypeId) {
        try {
            final Instant start = Instant.now();
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            final SearchResult
                    searchResult =
                    restTemplate.exchange(withVersionQueryParam(applicationParams.displayWorkbasketDefURL(caseTypeId), version),
                            HttpMethod.GET,
                            requestEntity,
                            SearchResult.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            LOG.debug("Rest API getWizardPageCollection called for {}, finished in {}",
                    caseTypeId,
                    duration.toMillis());
            appInsights.trackDependency(CASE_DEFINITION, "WorkbasketResult", duration.toMillis(), true);
            return searchResult;
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                    "Problem getting WorkBasketResult definition for case type: %s because of %s",
                    caseTypeId,
                    e.getMessage()));
        }
    }

    private URI withVersionQueryParam(String url, int version) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParam("version", version);
        return builder.build().encode().toUri();
    }


}
