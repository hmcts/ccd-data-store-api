package uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Slf4j
@Component
public class LocationMatcher implements AttributeMatcher {

    @Override
    public boolean matchAttribute(RoleAssignmentFilteringResult result, CaseDetails caseDetails) {
        RoleAssignment roleAssignment = result.getRoleAssignment();
        log.debug("Apply filter on location {} for role assignment {}",
            roleAssignment.getAttributes().getLocation(),
            roleAssignment.getId());
        result.getRoleMatchingResult()
            .setValidLocation(isValuesMatching(roleAssignment.getAttributes().getLocation(), ""));
        return result.getRoleMatchingResult().isValidLocation();
    }
}
