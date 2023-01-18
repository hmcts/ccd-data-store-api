package uk.gov.hmcts.ccd.v2.external.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;

import java.util.List;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class InvalidCaseSupplementaryDataResponse {

    @JsonProperty("case_ids")
    private List<String> caseIds;

    @JsonProperty("case_users")
    private List<CaseAssignedUserRole> caseAssignedUserRoles;

    public InvalidCaseSupplementaryDataResponse(List<String> caseIds,
                                                List<CaseAssignedUserRole> caseAssignedUserRoles) {
        this.caseIds = caseIds;
        this.caseAssignedUserRoles = caseAssignedUserRoles;
    }
}
