package uk.gov.hmcts.ccd.data.definition;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;

@Named
@Singleton
public class UIDefinitionRepository {

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CachedUIDefinitionGateway cachedUiDefinitionGateway;

    @Inject
    UIDefinitionRepository(@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                           final CaseDefinitionRepository caseDefinitionRepository,
                           CachedUIDefinitionGateway cachedUiDefinitionGateway) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.cachedUiDefinitionGateway = cachedUiDefinitionGateway;
    }

    public SearchResult getWorkBasketResult(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getWorkBasketResult(version.getVersion(), caseTypeId);
    }

    public SearchResult getSearchResult(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getSearchResult(version.getVersion(), caseTypeId);
    }

    public SearchInputDefinition getSearchInputDefinitions(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getSearchInputDefinitions(version.getVersion(), caseTypeId);
    }

    public List<WizardPage> getWizardPageCollection(final String caseTypeId, final String eventTriggerId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getWizardPageCollection(version.getVersion(), caseTypeId, eventTriggerId);
    }

    public WorkbasketInputDefinition getWorkbasketInputDefinitions(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getWorkbasketInputDefinitions(version.getVersion(), caseTypeId);
    }

    public CaseTabCollection getCaseTabCollection(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getCaseTabCollection(version.getVersion(), caseTypeId);
    }

    public BannersResult getBanners(final List<String> jurisdictionReferences) {
        return cachedUiDefinitionGateway.getBanners(jurisdictionReferences);
    }
    
    public JurisdictionUiConfigResult getJurisdictionUiConfigs(final List<String> jurisdictionReferences) {
        return cachedUiDefinitionGateway.getJurisdictionUiConfigs(jurisdictionReferences);
    }

}
