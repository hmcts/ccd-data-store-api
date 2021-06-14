package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

@Slf4j
@Component
public class BeginDateEndDateMatcher implements RoleAttributeMatcher {

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        return matchBeginEndDates(roleAssignment);
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {
        return matchBeginEndDates(roleAssignment);
    }

    private boolean matchBeginEndDates(RoleAssignment roleAssignment) {
        log.debug("Apply filter on start {} and end time {} for role assignment {}",
            roleAssignment.getBeginTime(),
            roleAssignment.getEndTime(),
            roleAssignment.getId());
        if (roleAssignment.getBeginTime() != null && roleAssignment.getEndTime() != null) {
            Instant now = Instant.now();
            return roleAssignment.getBeginTime().isBefore(now)
                && roleAssignment.getEndTime().isAfter(now);
        }
        return false;
    }
}
