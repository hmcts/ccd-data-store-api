package uk.gov.hmcts.ccd.v2.external.resource;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseAssignedUserRolesResource extends RepresentationModel<CaseAssignedUserRolesResource> {

    @JsonProperty("case_users")
    private List<CaseAssignedUserRole> caseAssignedUserRoles;

    public CaseAssignedUserRolesResource(List<CaseAssignedUserRole> caseAssignedUserRoles) {
        this.caseAssignedUserRoles = caseAssignedUserRoles;
    }
}
