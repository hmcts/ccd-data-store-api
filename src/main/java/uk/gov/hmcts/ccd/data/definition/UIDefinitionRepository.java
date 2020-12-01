package uk.gov.hmcts.ccd.data.definition;

import org.springframework.beans.factory.annotation.Qualifier;
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

    public SearchResultDefinition getWorkBasketResult(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getWorkBasketResult(version.getVersion(), caseTypeId);
    }

    public SearchResultDefinition getSearchResult(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getSearchResult(version.getVersion(), caseTypeId);
    }

    public SearchResultDefinition getSearchCasesResult(String caseTypeId, String useCase) {
        CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getSearchCasesResultDefinition(version.getVersion(), caseTypeId, useCase);
    }

    public SearchInputFieldsDefinition getSearchInputFieldDefinitions(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getSearchInputFieldDefinitions(version.getVersion(), caseTypeId);
    }

    public List<WizardPage> getWizardPageCollection(final String caseTypeId, final String eventId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getWizardPageCollection(version.getVersion(), caseTypeId, eventId);
    }

    public WorkbasketInputFieldsDefinition getWorkbasketInputDefinitions(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getWorkbasketInputFieldsDefinitions(version.getVersion(), caseTypeId);
    }

    public CaseTypeTabsDefinition getCaseTabCollection(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getCaseTypeTabsCollection(version.getVersion(), caseTypeId);
    }

    public BannersResult getBanners(final List<String> jurisdictionReferences) {
        return cachedUiDefinitionGateway.getBanners(jurisdictionReferences);
    }

    public JurisdictionUiConfigResult getJurisdictionUiConfigs(final List<String> jurisdictionReferences) {
        return cachedUiDefinitionGateway.getJurisdictionUiConfigs(jurisdictionReferences);
    }

}
