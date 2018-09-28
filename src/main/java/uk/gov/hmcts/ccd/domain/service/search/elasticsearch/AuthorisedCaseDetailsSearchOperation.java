package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.List;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDataFilter;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

@Service
@Qualifier(AuthorisedCaseDetailsSearchOperation.QUALIFIER)
public class AuthorisedCaseDetailsSearchOperation implements CaseDetailsSearchOperation {

    public static final String QUALIFIER = "AuthorisedCaseDetailsSearchOperation";

    private final CaseDetailsSearchOperation caseDetailsSearchOperation;
    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;
    private final AuthorisedCaseDataFilter authorisedCaseDataFilter;

    public AuthorisedCaseDetailsSearchOperation(
        @Qualifier(ElasticsearchCaseDetailsSearchOperation.QUALIFIER) CaseDetailsSearchOperation caseDetailsSearchOperation,
        AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService,
        AuthorisedCaseDataFilter authorisedCaseDataFilter) {

        this.caseDetailsSearchOperation = caseDetailsSearchOperation;
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
        this.authorisedCaseDataFilter = authorisedCaseDataFilter;
    }

    @Override
    public CaseDetailsSearchResult execute(String caseTypeId, String jsonQuery) {
        return authorisedCaseDefinitionDataService
            .getAuthorisedCaseType(caseTypeId, CAN_READ)
            .map(caseType -> {
                CaseDetailsSearchResult result = search(caseType, jsonQuery);
                filterFieldsByAccess(caseType, result.getCases());
                return result;
            })
            .orElse(CaseDetailsSearchResult.EMPTY);
    }

    private CaseDetailsSearchResult search(CaseType caseType, String jsonQuery) {
        return caseDetailsSearchOperation.execute(caseType.getId(), jsonQuery);
    }

    private void filterFieldsByAccess(CaseType caseType, List<CaseDetails> cases) {
        cases.forEach(caseDetails -> authorisedCaseDataFilter.filterFields(caseType, caseDetails));
    }

}
