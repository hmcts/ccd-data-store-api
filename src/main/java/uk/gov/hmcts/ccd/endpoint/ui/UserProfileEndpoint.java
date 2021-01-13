package uk.gov.hmcts.ccd.endpoint.ui;

import javax.inject.Inject;
import javax.transaction.Transactional;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@RestController
public class UserProfileEndpoint {
    private final GetUserProfileOperation getUserProfileOperation;

    @Inject
    public UserProfileEndpoint(@Qualifier(AuthorisedGetUserProfileOperation.QUALIFIER)
                                   final GetUserProfileOperation getUserProfileOperation) {
        this.getUserProfileOperation = getUserProfileOperation;
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/profile", method = RequestMethod.GET)
    @ApiOperation(value = "Get default setting for user")
    @ApiResponse(code = 200, message = "User default settings")
    public UserProfile getUserProfile() {
        return getUserProfileOperation.execute(AccessControlService.CAN_READ);
    }
}
