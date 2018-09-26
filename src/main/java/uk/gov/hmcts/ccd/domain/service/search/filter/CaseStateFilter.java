package uk.gov.hmcts.ccd.domain.service.search.filter;

import java.util.List;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import uk.gov.hmcts.ccd.domain.service.common.AuthorisedCaseDefinitionDataService;

public class CaseStateFilter {
    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    public CaseStateFilter(AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService) {
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
    }

    public final List<String> getCaseStateIdsForUserReadAccess(String caseTypeId) {
        return authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(caseTypeId, CAN_READ);
    }
}
