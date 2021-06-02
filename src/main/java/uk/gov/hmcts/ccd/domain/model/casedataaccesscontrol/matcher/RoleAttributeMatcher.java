package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleMatchingResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface RoleAttributeMatcher {

    String CASE_MANAGEMENT__LOCATION = "caseManagementLocation";
    String BASE_LOCATION = "baseLocation";
    String REGION = "region";
    String EMPTY_STR = "";


    void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPair, CaseDetails caseDetails);

    void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPair, CaseTypeDefinition caseTypeDefinition);

    default boolean isValuesMatching(Optional<String> roleAssignmentValue,
                                     String caseDataValue) {
        if (roleAssignmentValue == null) {
            return true;
        }
        return roleAssignmentValue.isEmpty()
            || roleAssignmentValue.get().equals(caseDataValue);
    }
}
