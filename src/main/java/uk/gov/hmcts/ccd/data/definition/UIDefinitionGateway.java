package uk.gov.hmcts.ccd.data.definition;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputFieldsDefinition;

public interface UIDefinitionGateway {

    SearchResult getWorkBasketResult(int version, String caseTypeId);

    SearchResult getSearchResult(int version, String caseTypeId);

    SearchInputFieldsDefinition getSearchInputDefinitions(int version, String caseTypeId);

    WorkbasketInputFieldsDefinition getWorkbasketInputDefinitions(int version, String caseTypeId);

    CaseTypeTabsDefinition getCaseTabCollection(int version, String caseTypeId);

    List<WizardPage> getWizardPageCollection(int version, String caseTypeId, String eventTriggerId);

    BannersResult getBanners(final List<String> jurisdictionIds);

	JurisdictionUiConfigResult getJurisdictionUiConfigs(List<String> jurisdictionIds);
}
