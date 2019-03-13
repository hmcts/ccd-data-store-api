package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.UserService;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;

@Service
@Qualifier(DefaultGetUserProfileOperation.QUALIFIER)
public class DefaultGetUserProfileOperation implements GetUserProfileOperation {
    public static final String QUALIFIER = "default";
    private final UserService userService;

    public DefaultGetUserProfileOperation(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserProfile execute(Predicate<AccessControlList> access) {
        return userService.getUserProfile();
    }
}
