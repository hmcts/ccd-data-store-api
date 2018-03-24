package uk.gov.hmcts.ccd.endpoint.ui;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;

import javax.inject.Inject;
import javax.transaction.Transactional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
public class UserProfileEndpoint {
    private static final String BEARER = "Bearer ";
    private final GetUserProfileOperation getUserProfileOperation;

    @Inject
    public UserProfileEndpoint(@Qualifier(DefaultGetUserProfileOperation.QUALIFIER) final GetUserProfileOperation getUserProfileOperation) {
        this.getUserProfileOperation = getUserProfileOperation;
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/profile", method = RequestMethod.GET)
    @ApiOperation(value = "Get default setting for user")
    @ApiResponse(code = 200, message = "User default settings")
    public UserProfile getUserProfile(@RequestHeader(value = AUTHORIZATION) final String authHeader) {
        final String userToken = authHeader.substring(BEARER.length());
        return getUserProfileOperation.execute(userToken);
    }
}
