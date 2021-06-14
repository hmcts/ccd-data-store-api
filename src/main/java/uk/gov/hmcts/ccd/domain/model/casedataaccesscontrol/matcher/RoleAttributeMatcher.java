package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.util.Optional;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface RoleAttributeMatcher {

    String CASE_MANAGEMENT__LOCATION = "caseManagementLocation";
    String BASE_LOCATION = "baseLocation";
    String REGION = "region";
    String EMPTY_STR = "";


    boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails);

    default boolean matchAttribute(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {
        return true;
    }

    default boolean isValuesMatching(Optional<String> roleAssignmentValue,
                                     String caseDataValue) {
        if (roleAssignmentValue == null) {
            return true;
        }
        return roleAssignmentValue.isEmpty()
            || roleAssignmentValue.get().equals(caseDataValue);
    }
}
