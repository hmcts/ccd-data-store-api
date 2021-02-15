package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class RoleAssignmentResponse {
    @JsonProperty("roleAssignmentResponse")
    private List<RoleAssignmentDTO> roleAssignments;
}
