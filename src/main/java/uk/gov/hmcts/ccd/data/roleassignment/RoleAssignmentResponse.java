package uk.gov.hmcts.ccd.data.roleassignment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class RoleAssignmentResponse {
    @JsonProperty("roleAssignmentResponse")
    private List<RoleAssignmentRecord> roleAssignmentRecords;
}
