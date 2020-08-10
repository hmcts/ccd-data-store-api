package uk.gov.hmcts.ccd.domain.service.cauroles.rolevalidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Qualifier("default")
public class DefaultCaseAssignedUserRoleValidator implements CaseAssignedUserRoleValidator {

    private UserRepository userRepository;
    private ApplicationParams applicationParams;

    @Autowired
    public DefaultCaseAssignedUserRoleValidator(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                                ApplicationParams applicationParams) {
        this.userRepository = userRepository;
        this.applicationParams = applicationParams;
    }

    public boolean canAccessUserCaseRoles(List<String> userIds) {
        boolean canAccess = userRepository.anyRoleEqualsAnyOf(applicationParams.getCcdAccessControlCrossJurisdictionRoles());
        if (!canAccess) {
            userIds = userIds.stream().distinct().collect(Collectors.toList());
            canAccess = userIds.size() == 1 && userIds.contains(this.userRepository.getUserId());
        }
        return canAccess;
    }
}
