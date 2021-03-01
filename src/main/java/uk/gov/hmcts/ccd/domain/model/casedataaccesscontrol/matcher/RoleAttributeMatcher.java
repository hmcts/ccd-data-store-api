package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.util.Optional;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

public interface RoleAttributeMatcher {

    void matchAttribute(RoleAssignmentFilteringResult result, CaseDetails caseDetails);

    default boolean isValuesMatching(Optional<String> roleAssignmentValue,
                                     String caseDataValue) {
        if (roleAssignmentValue == null) {
            return true;
        }
        return roleAssignmentValue.get() == null
            || roleAssignmentValue.get().equals(caseDataValue);
    }
}
