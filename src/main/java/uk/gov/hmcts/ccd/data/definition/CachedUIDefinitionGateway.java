package uk.gov.hmcts.ccd.data.definition;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;

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
    public SearchResult getWorkBasketResult(final int version, final String caseTypeId) {
        LOG.debug("remote retrieving version {} of workbasket result for {}", version, caseTypeId);
        return httpUiDefinitionGateway.getWorkBasketResult(version, caseTypeId);
    }

    @Override
    @Cacheable("searchResultCache")
    public SearchResult getSearchResult(final int version, final String caseTypeId) {
        LOG.debug("remote retrieving version {} of search result for {}", version, caseTypeId);
        return httpUiDefinitionGateway.getSearchResult(version, caseTypeId);
    }

    @Override
    @Cacheable("searchInputDefinitionCache")
    public SearchInputDefinition getSearchInputDefinitions(final int version, final String caseTypeId) {
        LOG.debug("remote retrieving version {} of search input definitions for {}", version, caseTypeId);
        return httpUiDefinitionGateway.getSearchInputDefinitions(version, caseTypeId);
    }

    @Override
    @Cacheable("workbasketInputDefinitionCache")
    public WorkbasketInputDefinition getWorkbasketInputDefinitions(final int version, final String caseTypeId) {
        LOG.debug("remote retrieving version {} of workbasket input definitions for {}", version, caseTypeId);
        return httpUiDefinitionGateway.getWorkbasketInputDefinitions(version, caseTypeId);
    }

    @Override
    @Cacheable("caseTabCollectionCache")
    public CaseTabCollection getCaseTabCollection(final int version, final String caseTypeId) {
        LOG.debug("remote retrieving version {} of case tab collection for {}", version, caseTypeId);
        return httpUiDefinitionGateway.getCaseTabCollection(version, caseTypeId);
    }

    @Override
    @Cacheable("wizardPageCollectionCache")
    public List<WizardPage> getWizardPageCollection(final int version, final String caseTypeId, final String eventTriggerId) {
        LOG.debug("remote retrieving version {} of wizard page collection for {} - {}", version, caseTypeId, eventTriggerId);
        return httpUiDefinitionGateway.getWizardPageCollection(version, caseTypeId, eventTriggerId);
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
