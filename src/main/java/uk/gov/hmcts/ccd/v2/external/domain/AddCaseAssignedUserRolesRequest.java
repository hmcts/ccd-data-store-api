package uk.gov.hmcts.ccd.v2.external.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AddCaseAssignedUserRolesRequest {

    @JsonProperty("case_users")
    private List<CaseAssignedUserRoleWithOrganisation> caseAssignedUserRoles;

}
