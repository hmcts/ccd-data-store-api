package uk.gov.hmcts.ccd.domain.service.cauroles.rolevalidator;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;

@Service
@Qualifier("default")
public class DefaultCaseAssignedUserRoleValidator implements CaseAssignedUserRoleValidator {

    private final String roleCaseWorkerCaa = "caseworker-caa";

    private UserRepository userRepository;

    @Autowired
    public DefaultCaseAssignedUserRoleValidator(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean canAccessUserCaseRoles(List<String> userIds) {
        boolean canAccess = this.userRepository.getUserRoles().contains(roleCaseWorkerCaa);
        if (!canAccess) {
            userIds = userIds.stream().distinct().collect(Collectors.toList());
            canAccess = userIds.size() == 1 && userIds.contains(this.userRepository.getUserId());
        }
        return canAccess;
    }
}
