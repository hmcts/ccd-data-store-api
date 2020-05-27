package uk.gov.hmcts.ccd.data.definition;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageCollection;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.search.UseCase;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

/**
 * NOTE: We want to cache definitions, so the only client of this class should be CachedUIDefinitionGateway.
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

    @Inject
    HttpUIDefinitionGateway(final ApplicationParams applicationParams,
                            final SecurityUtils securityUtils,
                            final RestTemplate restTemplate) {
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
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
            return searchResult;
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                    "Problem getting SearchResult definition for case type: %s because of %s",
                    caseTypeId,
                    e.getMessage()));
        }
    }

    @Override
    public SearchInputFieldsDefinition getSearchInputFieldDefinitions(int version, String caseTypeId) {
        try {
            final Instant start = Instant.now();
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            final SearchInputFieldsDefinition
                    definition =
                    restTemplate.exchange(withVersionQueryParam(applicationParams.searchInputDefinition(caseTypeId), version),
                            HttpMethod.GET,
                            requestEntity,
                            SearchInputFieldsDefinition.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            LOG.debug("Rest API getSearchInputDefinitionsGetHttp called for {}, finished in {}",
                    caseTypeId,
                    duration.toMillis());
            return definition;
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                    "Problem getting SearchInputs definition for case type: %s because of %s",
                    caseTypeId,
                    e.getMessage()));
        }
    }

    @Override
    public WorkbasketInputFieldsDefinition getWorkbasketInputFieldsDefinitions(int version, String caseTypeId) {
        try {
            final Instant start = Instant.now();
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            final WorkbasketInputFieldsDefinition
                    definition =
                    restTemplate.exchange(withVersionQueryParam(applicationParams.workbasketInputDefinition(caseTypeId), version),
                            HttpMethod.GET,
                            requestEntity,
                            WorkbasketInputFieldsDefinition.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            LOG.debug("Rest API getWorkbasketInputDefinitionsGetHttp called for {}, finished in {}",
                    caseTypeId,
                    duration.toMillis());
            return definition;
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                    "Problem getting WorkbasketInputs definition for case type: %s because of %s",
                    caseTypeId,
                    e.getMessage()));
        }
    }

    @Override
    public CaseTypeTabsDefinition getCaseTypeTabsCollection(int version, String caseTypeId) {
        final Instant start = Instant.now();
        final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
        final CaseTypeTabsDefinition
                collection =
                restTemplate.exchange(withVersionQueryParam(applicationParams.displayCaseTabCollection(caseTypeId), version),
                        HttpMethod.GET,
                        requestEntity,
                        CaseTypeTabsDefinition.class).getBody();
        final Duration duration = Duration.between(start, Instant.now());
        LOG.debug("Rest API getCaseTypeTabsCollection called for {}, finished in {}",
                caseTypeId,
                duration.toMillis());
        return collection;
    }

    @Override
    public List<WizardPage> getWizardPageCollection(int version, String caseTypeId, String eventId) {
        final Instant start = Instant.now();
        final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
        final WizardPageCollection
                wpc =
                restTemplate.exchange(withVersionQueryParam(applicationParams.displayWizardPageCollection(caseTypeId, eventId), version),
                        HttpMethod.GET,
                        requestEntity,
                        WizardPageCollection.class).getBody();
        final Duration duration = Duration.between(start, Instant.now());
        LOG.debug("Rest API getWizardPageCollectionGetHttp called for {}, finished in {}",
                caseTypeId,
                duration.toMillis());
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
            LOG.debug("Rest API getWorkBasketResultGetHttp called for {}, finished in {}",
                    caseTypeId,
                    duration.toMillis());
            return searchResult;
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                    "Problem getting WorkBasketResult definition for case type: %s because of %s",
                    caseTypeId,
                    e.getMessage()));
        }
    }

    @Override
    public SearchResult getSearchCasesResult(int version, String caseTypeId, UseCase useCase) {
        try {
            final Instant start = Instant.now();
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            final SearchResult
                searchResult =
                restTemplate.exchange(withVersionQueryParam(applicationParams.displaySearchCasesResultDefURL(caseTypeId, useCase), version),
                    HttpMethod.GET,
                    requestEntity,
                    SearchResult.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            LOG.debug("Rest API getSearchCasesResultGetHttp called for {}, finished in {}",
                caseTypeId,
                duration.toMillis());
            return searchResult;
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                "Problem getting SearchCasesResult definition for case type: %s because of %s",
                caseTypeId,
                e.getMessage()));
        }
    }

    private URI withVersionQueryParam(String url, int version) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParam("version", version);
        return builder.build().encode().toUri();
    }

    private URI withJurisdictionIds(String url, final List<String> jurisdictionIds) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParam("ids", String.join(",", jurisdictionIds));
        return builder.build().encode().toUri();
    }

    public BannersResult getBanners(final List<String> jurisdictionIds) {
        try {
            final Instant start = Instant.now();
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            final BannersResult
                bannersResult =
                restTemplate.exchange(withJurisdictionIds(applicationParams.bannersURL(), jurisdictionIds),
                    HttpMethod.GET,
                    requestEntity,
                    BannersResult.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            LOG.debug("Rest API getBanners called for {}, finished in {}",
                jurisdictionIds,
                duration.toMillis());
            return bannersResult;
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                "Problem getting banners for jurisdiction references: %s because of %s",
                jurisdictionIds,
                e.getMessage()));
        }
    }

    public JurisdictionUiConfigResult getJurisdictionUiConfigs(final List<String> jurisdictionIds) {
        try {
            final Instant start = Instant.now();
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            final JurisdictionUiConfigResult
                jurisdictionUiConfigResult =
                restTemplate.exchange(withJurisdictionIds(applicationParams.jurisdictionUiConfigsURL(), jurisdictionIds),
                    HttpMethod.GET,
                    requestEntity,
                    JurisdictionUiConfigResult.class).getBody();
            final Duration duration = Duration.between(start, Instant.now());
            LOG.debug("Rest API getJurisdictionUiConfigs called for {}, finished in {}",
                jurisdictionIds,
                duration.toMillis());
            return jurisdictionUiConfigResult;
        } catch (final Exception e) {
            throw new ServiceException(String.format(
                "Problem getting jurisdiction UI configs for jurisdiction references: %s because of %s",
                jurisdictionIds,
                e.getMessage()));
        }
    }
}
