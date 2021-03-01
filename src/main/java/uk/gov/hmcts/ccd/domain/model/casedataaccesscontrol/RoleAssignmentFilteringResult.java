package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.AllArgsConstructor;
import lombok.Data;

import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType.CHALLENGED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType.SPECIFIC;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType.STANDARD;

@Data
@AllArgsConstructor
public class RoleAssignmentFilteringResult {
    private final RoleAssignment roleAssignment;
    private final RoleMatchingResult roleMatchingResult;

    public AccessProcess getAccessProcess(String grantType) {
        if (STANDARD.name().equals(grantType)
            && SPECIFIC.name().equals(grantType)
            && CHALLENGED.name().equals(grantType)
            && roleMatchingResult.matchedAllValues()) {
            return AccessProcess.NONE;
        } else if (STANDARD.name().equals(grantType)
            && roleMatchingResult.matchedAExceptRegionAndLocation()) {
            return AccessProcess.CHALLENGED;
        }
        return AccessProcess.SPECIFIC;
    }
}
