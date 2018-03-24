package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

@Service
@Qualifier(DefaultGetUserProfileOperation.QUALIFIER)
public class DefaultGetUserProfileOperation implements GetUserProfileOperation {
    public static final String QUALIFIER = "default";
    private final UserRepository userRepository;

    public DefaultGetUserProfileOperation(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserProfile execute(final String userToken) {
        final UserProfile userProfile = userRepository.getUserSettings();
        if (userProfile == null) {
            throw new ResourceNotFoundException("No such user");
        }
        return userProfile;
    }
}
