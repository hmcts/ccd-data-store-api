package uk.gov.hmcts.ccd.data.definition;

import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputFieldsDefinition;

import java.util.List;

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
