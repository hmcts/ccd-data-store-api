package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoleAssignmentFilteringResult {
    private final RoleAssignment roleAssignment;
    private final RoleMatchingResult roleMatchingResult;
}
