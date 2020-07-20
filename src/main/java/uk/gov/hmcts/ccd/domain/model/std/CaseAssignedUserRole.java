package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CaseAssignedUserRole {

    public CaseAssignedUserRole(String caseDataId, String userId, String caseRole) {
        this.caseDataId = caseDataId;
        this.userId = userId;
        this.caseRole = caseRole;
    }

    @JsonProperty("case_id")
    private String caseDataId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("case_role")
    private String caseRole;

    @JsonProperty("organisation_id")
    @JsonInclude(Include.NON_EMPTY)
    private String organisationId;
}
