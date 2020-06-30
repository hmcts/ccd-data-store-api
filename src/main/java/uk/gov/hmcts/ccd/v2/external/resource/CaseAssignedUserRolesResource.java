package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseAssignedUserRolesResource extends RepresentationModel<RepresentationModel<?>> {

    @JsonProperty("case_users")
    private List<CaseAssignedUserRole> caseAssignedUserRoles;

    public CaseAssignedUserRolesResource(List<CaseAssignedUserRole> caseAssignedUserRoles) {
        this.caseAssignedUserRoles = caseAssignedUserRoles;
    }

    @Override
    @JsonIgnore
    public Links getLinks() {
        return super.getLinks();
    }

}
