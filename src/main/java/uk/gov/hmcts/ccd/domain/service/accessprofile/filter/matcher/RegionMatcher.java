package uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Slf4j
@Component
public class RegionMatcher implements AttributeMatcher {

    @Override
    public boolean matchAttribute(RoleAssignmentFilteringResult result, CaseDetails caseDetails) {
        RoleAssignment roleAssignment = result.getRoleAssignment();
        log.debug("Apply filter on region {} for role assignment {}",
            roleAssignment.getAttributes().getRegion(),
            roleAssignment.getId());
        result.getRoleMatchingResult()
            .setValidRegion(isValuesMatching(roleAssignment.getAttributes().getRegion(), ""));
        return result.getRoleMatchingResult().isValidRegion();
    }
}
