package uk.gov.hmcts.ccd.data.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputFieldsDefinition;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;

@Named
@Qualifier("Cache")
@Singleton
public class CachedUIDefinitionGateway implements UIDefinitionGateway {

    private static final Logger LOG = LoggerFactory.getLogger(CachedUIDefinitionGateway.class);
    private UIDefinitionGateway httpUiDefinitionGateway;

    @Inject
    CachedUIDefinitionGateway(@Qualifier("Http") UIDefinitionGateway httpUiDefinitionGateway) {
        this.httpUiDefinitionGateway = httpUiDefinitionGateway;
    }

    @Override
    @Cacheable("workBasketResultCache")
    public SearchResultDefinition getWorkBasketResult(final int version, final String caseTypeId) {
        LOG.debug("remote retrieving version {} of workbasket result for {}", version, caseTypeId);
        return httpUiDefinitionGateway.getWorkBasketResult(version, caseTypeId);
    }

    @Override
    @Cacheable("searchResultCache")
    public SearchResultDefinition getSearchResult(final int version, final String caseTypeId) {
        LOG.debug("remote retrieving version {} of search result for {}", version, caseTypeId);
        return httpUiDefinitionGateway.getSearchResult(version, caseTypeId);
    }

    @Override
    @Cacheable("searchCasesResultCache")
    public SearchResultDefinition getSearchCasesResultDefinition(int version, String caseTypeId, String useCase) {
        LOG.debug("remote retrieving version {} of search cases result definition for {}", version, caseTypeId);
        return httpUiDefinitionGateway.getSearchCasesResultDefinition(version, caseTypeId, useCase);
    }

    @Override
    @Cacheable("searchInputDefinitionCache")
    public SearchInputFieldsDefinition getSearchInputFieldDefinitions(final int version, final String caseTypeId) {
        LOG.debug("remote retrieving version {} of search input definitions for {}", version, caseTypeId);
        return httpUiDefinitionGateway.getSearchInputFieldDefinitions(version, caseTypeId);
    }

    @Override
    @Cacheable("workbasketInputDefinitionCache")
    public WorkbasketInputFieldsDefinition getWorkbasketInputFieldsDefinitions(final int version,
                                                                               final String caseTypeId) {
        LOG.debug("remote retrieving version {} of workbasket input definitions for {}", version, caseTypeId);
        return httpUiDefinitionGateway.getWorkbasketInputFieldsDefinitions(version, caseTypeId);
    }

    @Override
    @Cacheable("caseTabCollectionCache")
    public CaseTypeTabsDefinition getCaseTypeTabsCollection(final int version, final String caseTypeId) {
        LOG.debug("remote retrieving version {} of case tab collection for {}", version, caseTypeId);
        return httpUiDefinitionGateway.getCaseTypeTabsCollection(version, caseTypeId);
    }

    @Override
    @Cacheable("wizardPageCollectionCache")
    public List<WizardPage> getWizardPageCollection(final int version, final String caseTypeId, final String eventId) {
        LOG.debug("remote retrieving version {} of wizard page collection for {} - {}", version, caseTypeId, eventId);
        return httpUiDefinitionGateway.getWizardPageCollection(version, caseTypeId, eventId);
    }

    @Override
    @Cacheable("bannersCache")
    public BannersResult getBanners(final List<String> jurisdictionIds) {
        return httpUiDefinitionGateway.getBanners(jurisdictionIds);
    }

    @Override
    @Cacheable("jurisdictionUiConfigsCache")
    public JurisdictionUiConfigResult getJurisdictionUiConfigs(final List<String> jurisdictionIds) {
        return httpUiDefinitionGateway.getJurisdictionUiConfigs(jurisdictionIds);
    }
}
