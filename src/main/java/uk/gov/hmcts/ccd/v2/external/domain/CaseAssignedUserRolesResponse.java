package uk.gov.hmcts.ccd.v2.external.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController;

@Data
@NoArgsConstructor
@Schema(description = "Add Case-Assigned User Roles Response")
public class CaseAssignedUserRolesResponse {

    public CaseAssignedUserRolesResponse(String status) {
        this.status = status;
    }

    @JsonProperty("status_message")
    @Schema(description = "Domain Status Message", required = true,
        example = CaseAssignedUserRolesController.ADD_SUCCESS_MESSAGE)
    private String status;

}

