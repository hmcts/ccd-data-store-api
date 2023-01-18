package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;

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
    private Attributes attributes;


    public RoleAssignmentQuery(List<String> caseIds, List<String> userIds) {
        this.actorId = userIds;
        this.attributes = Attributes.builder().caseId(caseIds).build();
        this.roleType = List.of(RoleType.CASE.name());
    }

    public RoleAssignmentQuery(String caseId, String userId, List<String> roleNames) {
        this.actorId = List.of(userId);
        this.attributes = Attributes.builder().caseId(List.of(caseId)).build();
        this.roleType = List.of(RoleType.CASE.name());
        this.roleName = roleNames;
    }

}
