package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.UserService;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;

@Service
@Qualifier(DefaultGetUserProfileOperation.QUALIFIER)
public class DefaultGetUserProfileOperation implements GetUserProfileOperation {
    public static final String QUALIFIER = "default";
    private final UserService userService;

    public DefaultGetUserProfileOperation(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public CompletableFuture<UserProfile> execute(final String userToken) {
        return userService.getUserProfileAsync();
    }
}
