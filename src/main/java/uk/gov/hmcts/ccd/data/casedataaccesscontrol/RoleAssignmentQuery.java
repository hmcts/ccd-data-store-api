package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleType;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleAssignmentQuery {

    private List<String> actorId;
    private List<String> roleType;
    private List<String> roleName;
    private List<String> classification;
    private List<String> grantType;
    private List<String> roleCategory;
    private List<String> validAt;
    private List<String> authorisations;
    private List<Attributes> attributes;


    public RoleAssignmentQuery(List<String> caseIds, List<String> userIds) {
        final var attribute = Attributes.builder().caseId(caseIds).build();
        final var attributes = new ArrayList<Attributes>();
        final ArrayList roleType = new ArrayList<Attributes>();

        attributes.add(attribute);
        roleType.add(RoleType.CASE.name());
        this.actorId = userIds;
        this.attributes = attributes;
        this.roleType = roleType;
    }
}
