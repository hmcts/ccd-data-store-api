package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.UserService;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;

@Service
@Qualifier(DefaultGetUserProfileOperation.QUALIFIER)
@Slf4j
public class DefaultGetUserProfileOperation implements GetUserProfileOperation {
    public static final String QUALIFIER = "default";
    private final UserService userService;

    public DefaultGetUserProfileOperation(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserProfile execute(Predicate<AccessControlList> access) {
        log.info("Get User profile");
        return userService.getUserProfile();
    }
}
