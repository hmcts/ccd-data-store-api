package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.util.Optional;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface RoleAttributeMatcher {

    String CASE_MANAGEMENT__LOCATION = "caseManagementLocation";
    String CASE_ACCESS_GROUPS = "CaseAccessGroups";
    String COLLECTION_VALUE_FIELD = "value";
    String COLLECTION_ID_FIELD = "id";
    String CASE_ACCESS_GROUP_ID_FIELD = "caseAccessGroupId";
    String BASE_LOCATION = "baseLocation";
    String REGION = "region";
    String EMPTY_STR = "";

    MatcherType getType();

    default boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        return true;
    }

    default boolean matchAttribute(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {
        return true;
    }

    @SuppressWarnings("java:S2789")
    default boolean isValuesMatching(Optional<String> roleAssignmentValue,
                                     String caseDataValue) {
        if (roleAssignmentValue == null) {
            return true;
        }
        return roleAssignmentValue.isEmpty()
            || roleAssignmentValue.get().equals(caseDataValue);
    }
}
