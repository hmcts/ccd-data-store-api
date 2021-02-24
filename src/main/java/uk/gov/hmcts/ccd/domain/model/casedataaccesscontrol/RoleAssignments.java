package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class RoleAssignments {
    private List<RoleAssignment> roleAssignments;
}
