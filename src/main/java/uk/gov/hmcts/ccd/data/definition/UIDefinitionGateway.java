package uk.gov.hmcts.ccd.data.definition;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.search.UseCase;

public interface UIDefinitionGateway {

    SearchResult getWorkBasketResult(int version, String caseTypeId);

    SearchResult getSearchResult(int version, String caseTypeId);

    SearchResult getSearchCasesResult(int version, String caseTypeId, UseCase useCase);

    SearchInputFieldsDefinition getSearchInputFieldDefinitions(int version, String caseTypeId);

    WorkbasketInputFieldsDefinition getWorkbasketInputFieldsDefinitions(int version, String caseTypeId);

    CaseTypeTabsDefinition getCaseTypeTabsCollection(int version, String caseTypeId);

    List<WizardPage> getWizardPageCollection(int version, String caseTypeId, String eventId);

    BannersResult getBanners(final List<String> jurisdictionIds);

    JurisdictionUiConfigResult getJurisdictionUiConfigs(List<String> jurisdictionIds);

}
