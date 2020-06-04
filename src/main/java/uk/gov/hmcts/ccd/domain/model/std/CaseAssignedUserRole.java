package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CaseAssignedUserRole {

    @JsonProperty("case_data_id")
    private Long caseDataId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("case_role")
    private String caseRole;

    public Long getCaseDataId() {
        return caseDataId;
    }

    public String getUserId() {
        return userId;
    }

    public String getCaseRole() {
        return caseRole;
    }
}
