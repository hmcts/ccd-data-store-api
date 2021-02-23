package uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher;

import java.util.Optional;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

public interface AttributeMatcher {

    boolean matchAttribute(RoleAssignmentFilteringResult result, CaseDetails caseDetails);

    default boolean isValuesMatching(Optional<String> roleAssignmentValue,
                                     String caseDataValue) {
        if (roleAssignmentValue == null) {
            return true;
        }
        return roleAssignmentValue.get() == caseDataValue
            || roleAssignmentValue.get().equals(caseDataValue);
    }
}
