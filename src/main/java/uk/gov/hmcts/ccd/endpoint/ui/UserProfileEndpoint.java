package uk.gov.hmcts.ccd.endpoint.ui;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import jakarta.inject.Inject;

@RestController
public class UserProfileEndpoint {
    private final GetUserProfileOperation getUserProfileOperation;

    @Inject
    public UserProfileEndpoint(@Qualifier(AuthorisedGetUserProfileOperation.QUALIFIER)
                                   final GetUserProfileOperation getUserProfileOperation) {
        this.getUserProfileOperation = getUserProfileOperation;
    }

    @RequestMapping(value = "/caseworkers/{uid}/profile", method = RequestMethod.GET)
    @Operation(summary = "Get default setting for user")
    @ApiResponse(responseCode = "200", description = "User default settings")
    public UserProfile getUserProfile() {
        return getUserProfileOperation.execute(AccessControlService.CAN_READ);
    }
}
