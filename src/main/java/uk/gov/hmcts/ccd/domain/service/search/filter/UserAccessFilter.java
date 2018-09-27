package uk.gov.hmcts.ccd.domain.service.search.filter;

import java.util.List;
import java.util.Optional;

import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

public class UserAccessFilter {

    private final CaseAccessService caseAccessService;

    public UserAccessFilter(CaseAccessService caseAccessService) {
        this.caseAccessService = caseAccessService;
    }

    public final Optional<List<Long>> getGrantedCaseIdsForRestrictedRoles() {
        return caseAccessService.getGrantedCaseIdsForRestrictedRoles();
    }
}
