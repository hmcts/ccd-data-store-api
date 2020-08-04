package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@ToString
@NoArgsConstructor
@Getter
public class CaseAssignedUserRoleWithOrganisation extends CaseAssignedUserRole {

    @JsonProperty("organisation_id")
    @JsonInclude(Include.NON_NULL)
    private String organisationId;

    public CaseAssignedUserRoleWithOrganisation(String caseDataId, String userId, String caseRole) {
        super(caseDataId, userId, caseRole);
    }

    public CaseAssignedUserRoleWithOrganisation(String caseDataId, String userId, String caseRole, String organisationId) {
        super(caseDataId, userId, caseRole);
        this.organisationId = organisationId;
    }
}
