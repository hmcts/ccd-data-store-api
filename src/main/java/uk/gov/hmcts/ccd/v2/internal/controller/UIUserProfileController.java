package uk.gov.hmcts.ccd.v2.internal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.UserProfileViewResource;

@RestController
@RequestMapping(path = "/internal")
public class UIUserProfileController {

    private final GetUserProfileOperation getUserProfileOperation;

    @Autowired
    public UIUserProfileController(
        @Qualifier(AuthorisedGetUserProfileOperation.QUALIFIER) final GetUserProfileOperation getUserProfileOperation) {
        this.getUserProfileOperation = getUserProfileOperation;
    }

    @GetMapping(
        path = "/profile",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_USER_PROFILE
        }
    )
    @Operation(
        summary = "Validate case data",
        description = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = CaseViewResource.class))
    )
    public ResponseEntity<UserProfileViewResource> getUserProfile() {

        UserProfile userProfile = getUserProfileOperation.execute(AccessControlService.CAN_READ);

        return ResponseEntity.ok(new UserProfileViewResource(userProfile));
    }
}
