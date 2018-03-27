package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.data.user.UserService;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

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
        final CompletableFuture<UserProfile> userProfile = userService.getUserProfileAsync().thenApply(profile -> {

                throw new ResourceNotFoundException("No such user");


        });
        return userProfile;
    }
}
