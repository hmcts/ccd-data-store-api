package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentRequestResponse {

    RoleAssignmentRequestResource roleAssignmentResponse;

}
