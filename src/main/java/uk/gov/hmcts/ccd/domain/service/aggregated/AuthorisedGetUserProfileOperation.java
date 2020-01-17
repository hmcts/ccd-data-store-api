package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Service
@Qualifier(AuthorisedGetUserProfileOperation.QUALIFIER)
public class AuthorisedGetUserProfileOperation implements GetUserProfileOperation {
    public static final String QUALIFIER = "authorised";

    private final UserRepository userRepository;
    private final AccessControlService accessControlService;
    private final GetUserProfileOperation getUserProfileOperation;

    public AuthorisedGetUserProfileOperation(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                             AccessControlService accessControlService,
                                             @Qualifier(DefaultGetUserProfileOperation.QUALIFIER) GetUserProfileOperation getUserProfileOperation) {
        this.accessControlService = accessControlService;
        this.getUserProfileOperation = getUserProfileOperation;
        this.userRepository = userRepository;
    }

    @Override
    public UserProfile execute(Predicate<AccessControlList> access) {
        return filterCaseTypes(getUserProfileOperation.execute(access), access);
    }

    private UserProfile filterCaseTypes(UserProfile userProfile, Predicate<AccessControlList> access) {
        final Set<String> userRoles = getUserRoles();
        Arrays.stream(userProfile.getJurisdictions()).forEach(
            jurisdiction -> jurisdiction.setCaseTypes(
                jurisdiction.getCaseTypes()
                    .stream()
                    .map(caseType -> verifyAccess(caseType, userRoles, access))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList())
            )
        );
        return userProfile;
    }

    private Set<String> getUserRoles() {
        return userRepository.getUserRoles();
    }

    private Optional<CaseType> verifyAccess(CaseType caseType, Set<String> userRoles, Predicate<AccessControlList> access) {
        if (caseType == null || CollectionUtils.isEmpty(userRoles)
            || !accessControlService.canAccessCaseTypeWithCriteria(caseType, userRoles, access)) {
            return Optional.empty();
        }
        caseType.setStates(accessControlService.filterCaseStatesByAccess(caseType.getStates(), userRoles, access));
        caseType.setEvents(accessControlService.filterCaseEventsByAccess(caseType.getEvents(), userRoles, access));

        return Optional.of(caseType);
    }
}
