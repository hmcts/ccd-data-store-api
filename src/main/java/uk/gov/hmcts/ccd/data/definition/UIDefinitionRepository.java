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
        SearchResultDefinition searchResultDefinition = cachedUiDefinitionGateway
            .getWorkBasketResult(version.getVersion(), caseTypeId);
        return cloneSearchResultDefinition(searchResultDefinition);
    }

    public SearchResultDefinition getSearchResult(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        SearchResultDefinition searchResultDefinition = cachedUiDefinitionGateway
            .getSearchResult(version.getVersion(), caseTypeId);
        return cloneSearchResultDefinition(searchResultDefinition);
    }

    public SearchResultDefinition getSearchCasesResult(String caseTypeId, String useCase) {
        CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        SearchResultDefinition searchResultDefinition = cachedUiDefinitionGateway
            .getSearchCasesResultDefinition(version.getVersion(), caseTypeId, useCase);
        return cloneSearchResultDefinition(searchResultDefinition);
    }

    private SearchResultDefinition cloneSearchResultDefinition(SearchResultDefinition searchResultDefinition) {
        return searchResultDefinition.createCopy();
    }

    public SearchInputFieldsDefinition getSearchInputFieldDefinitions(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return cachedUiDefinitionGateway.getSearchInputFieldDefinitions(version.getVersion(), caseTypeId);
    }

    public List<WizardPage> getWizardPageCollection(final String caseTypeId, final String eventId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        List<WizardPage> wizardPages = cachedUiDefinitionGateway
            .getWizardPageCollection(version.getVersion(), caseTypeId, eventId);
        return cloneWizardPages(wizardPages);
    }

    private List<WizardPage> cloneWizardPages(List<WizardPage> wizardPages) {
        return wizardPages.stream().map(WizardPage::createCopy).toList();
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
