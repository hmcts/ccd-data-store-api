package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseAssignedUserRolesResource extends RepresentationModel {

    @JsonProperty("case-users")
    private List<CaseAssignedUserRole> caseAssignedUserRoles;

    public CaseAssignedUserRolesResource(@NonNull String caseIds, String userIds, List<CaseAssignedUserRole> caseAssignedUserRoles) {
        this.caseAssignedUserRoles = caseAssignedUserRoles;
        add(linkTo(methodOn(CaseAssignedUserRolesController.class).getCaseUserRoles(caseIds, userIds)).withSelfRel());
    }
}
