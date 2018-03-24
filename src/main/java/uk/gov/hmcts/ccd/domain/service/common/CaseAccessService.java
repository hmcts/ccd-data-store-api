package uk.gov.hmcts.ccd.domain.service.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Check access to a case for the current user.
 *
 * User with the following roles should only be given access to the cases explicitly granted:
 * <ul>
 *     <li>caseworker-*-solicitor: Solicitors</li>
 *     <li>citizen(-loa[0-3]): Citizens</li>
 *     <li>letter-holder: Citizen with temporary user account, as per CMC journey</li>
 * </ul>
 */
@Service
public class CaseAccessService {

    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;

    private static final Pattern RESTRICT_GRANTED_ROLES_PATTERN
        = Pattern.compile(".+-solicitor$|^citizen(-.*)?$|^letter-holder$");

    public CaseAccessService(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                             CaseUserRepository caseUserRepository) {
        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;
    }

    public Boolean canUserAccess(CaseDetails caseDetails) {

        IDAMProperties currentUser = userRepository.getUserDetails();
        return !canOnlyViewGrantedCases(currentUser)
            || accessGranted(caseDetails, currentUser);

    }

    private Boolean accessGranted(CaseDetails caseDetails, IDAMProperties currentUser) {
        final List<Long> grantedCases = caseUserRepository.findCasesUserIdHasAccessTo(currentUser.getId());

        if (null != grantedCases && grantedCases.contains(caseDetails.getId())) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    private Boolean canOnlyViewGrantedCases(IDAMProperties currentUser) {
        return Stream.of(currentUser.getRoles())
                .anyMatch(role -> RESTRICT_GRANTED_ROLES_PATTERN.matcher(role).matches());
    }

}
