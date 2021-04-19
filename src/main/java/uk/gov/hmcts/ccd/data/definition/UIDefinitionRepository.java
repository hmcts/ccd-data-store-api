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
    private final UIDefinitionGateway uiDefinitionGateway;

    @Inject
    UIDefinitionRepository(@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                           final CaseDefinitionRepository caseDefinitionRepository,
                           @Qualifier(CachedUIDefinitionGateway.QUALIFIER)
                           final UIDefinitionGateway uiDefinitionGateway) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.uiDefinitionGateway = uiDefinitionGateway;
    }

    public SearchResultDefinition getWorkBasketResult(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return uiDefinitionGateway.getWorkBasketResult(version.getVersion(), caseTypeId);
    }

    public SearchResultDefinition getSearchResult(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return uiDefinitionGateway.getSearchResult(version.getVersion(), caseTypeId);
    }

    public SearchResultDefinition getSearchCasesResult(String caseTypeId, String useCase) {
        CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return uiDefinitionGateway.getSearchCasesResultDefinition(version.getVersion(), caseTypeId, useCase);
    }

    public SearchInputFieldsDefinition getSearchInputFieldDefinitions(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return uiDefinitionGateway.getSearchInputFieldDefinitions(version.getVersion(), caseTypeId);
    }

    public List<WizardPage> getWizardPageCollection(final String caseTypeId, final String eventId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return uiDefinitionGateway.getWizardPageCollection(version.getVersion(), caseTypeId, eventId);
    }

    public WorkbasketInputFieldsDefinition getWorkbasketInputDefinitions(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return uiDefinitionGateway.getWorkbasketInputFieldsDefinitions(version.getVersion(), caseTypeId);
    }

    public CaseTypeTabsDefinition getCaseTabCollection(final String caseTypeId) {
        final CaseTypeDefinitionVersion version = caseDefinitionRepository.getLatestVersion(caseTypeId);
        return uiDefinitionGateway.getCaseTypeTabsCollection(version.getVersion(), caseTypeId);
    }

    public BannersResult getBanners(final List<String> jurisdictionReferences) {
        return uiDefinitionGateway.getBanners(jurisdictionReferences);
    }

    public JurisdictionUiConfigResult getJurisdictionUiConfigs(final List<String> jurisdictionReferences) {
        return uiDefinitionGateway.getJurisdictionUiConfigs(jurisdictionReferences);
    }

}
