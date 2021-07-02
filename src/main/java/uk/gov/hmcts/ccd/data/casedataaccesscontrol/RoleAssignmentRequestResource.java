package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentRequestResource {

    @JsonProperty(value = "roleRequest")
    private RoleRequestResource request;

    @JsonProperty(value = "requestedRoles")
    private List<RoleAssignmentResource> requestedRoles;

}
