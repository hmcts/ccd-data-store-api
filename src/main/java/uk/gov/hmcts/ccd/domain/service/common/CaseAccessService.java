package uk.gov.hmcts.ccd.domain.service.common;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

/**
 * Check access to a case for the current user.
 *
 * <p>User with the following roles should only be given access to the cases explicitly granted:
 * <ul>
 * <li>caseworker-*-solicitor: Solicitors</li>
 * <li>citizen(-loa[0-3]): Citizens</li>
 * <li>letter-holder: Citizen with temporary user account, as per CMC journey</li>
 * </ul>
 */
@Service
public class CaseAccessService {

    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;

    private static final Pattern RESTRICT_GRANTED_ROLES_PATTERN
        = Pattern.compile(".+-solicitor$|.+-panelmember$|^citizen(-.*)?$|^letter-holder$|^caseworker-.+-localAuthority$");

    public CaseAccessService(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                             @Qualifier(CachedCaseUserRepository.QUALIFIER)  CaseUserRepository caseUserRepository) {
        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;
    }

    public Boolean canUserAccess(CaseDetails caseDetails) {
        if (canOnlyViewExplicitlyGrantedCases()) {
            return isExplicitAccessGranted(caseDetails);
        } else {
            return true;
        }
    }

    public AccessLevel getAccessLevel(UserInfo userInfo) {
        return userInfo.getRoles()
            .stream()
            .filter(role -> RESTRICT_GRANTED_ROLES_PATTERN.matcher(role).matches())
            .findFirst()
            .map(role -> AccessLevel.GRANTED)
            .orElse(AccessLevel.ALL);
    }

    public Optional<List<Long>> getGrantedCaseIdsForRestrictedRoles() {
        if (canOnlyViewExplicitlyGrantedCases()) {
            return Optional.of(caseUserRepository.findCasesUserIdHasAccessTo(userRepository.getUserId()));
        }

        return Optional.empty();
    }

    public Set<String> getCaseRoles(String caseId) {
        return new HashSet<>(caseUserRepository.findCaseRoles(Long.valueOf(caseId), userRepository.getUserId()));
    }

    public Set<String> getAccessRoles(String caseId) {
        return Sets.union(userRepository.getUserRoles(), getCaseRoles(caseId));
    }

    public Set<String> getCaseCreationCaseRoles() {
        return Collections.singleton(CREATOR.getRole());
    }

    public Set<String> getCaseCreationRoles() {
        return Sets.union(getUserRoles(), getCaseCreationCaseRoles());
    }

    public Set<String> getUserRoles() {
        Set<String> userRoles = userRepository.getUserRoles();
        if (userRoles == null) {
            throw new ValidationException("Cannot find user roles for the user");
        }
        return userRoles;
    }

    public boolean isJurisdictionAccessAllowed(String jurisdiction) {
        return this.userRepository
            .getUserRolesJurisdictions()
            .stream()
            .anyMatch(jurisdiction::equalsIgnoreCase);
    }


    public Boolean isExplicitAccessGranted(CaseDetails caseDetails) {
        final List<Long> grantedCases = caseUserRepository.findCasesUserIdHasAccessTo(userRepository.getUserId());

        if (null != grantedCases && grantedCases.contains(Long.valueOf(caseDetails.getId()))) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    public Boolean canOnlyViewExplicitlyGrantedCases() {
        return userRepository.anyRoleMatches(RESTRICT_GRANTED_ROLES_PATTERN);
    }

}
