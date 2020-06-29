package uk.gov.hmcts.ccd.data.definition;

import java.util.List;

import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;

public interface UIDefinitionGateway {

    SearchResultDefinition getWorkBasketResult(int version, String caseTypeId);

    SearchResultDefinition getSearchResult(int version, String caseTypeId);

    SearchResultDefinition getSearchCasesResultDefinition(int version, String caseTypeId, String useCase);

    SearchInputFieldsDefinition getSearchInputFieldDefinitions(int version, String caseTypeId);

    WorkbasketInputFieldsDefinition getWorkbasketInputFieldsDefinitions(int version, String caseTypeId);

    CaseTypeTabsDefinition getCaseTypeTabsCollection(int version, String caseTypeId);

    List<WizardPage> getWizardPageCollection(int version, String caseTypeId, String eventId);

    BannersResult getBanners(final List<String> jurisdictionIds);

    JurisdictionUiConfigResult getJurisdictionUiConfigs(List<String> jurisdictionIds);

}
