package uk.gov.hmcts.ccd.data.definition;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;

public interface UIDefinitionGateway {

    SearchResult getWorkBasketResult(int version, String caseTypeId);

    SearchResult getSearchResult(int version, String caseTypeId);

    SearchInputDefinition getSearchInputDefinitions(int version, String caseTypeId);

    WorkbasketInputDefinition getWorkbasketInputDefinitions(int version, String caseTypeId);

    CaseTabCollection getCaseTabCollection(int version, String caseTypeId);

    List<WizardPage> getWizardPageCollection(int version, String caseTypeId, String eventTriggerId);

    BannersResult getBanners(final List<String> jurisdictionIds);

	JurisdictionUiConfigResult getJurisdictionUiConfigs(List<String> jurisdictionIds);
}
