package uk.gov.hmcts.ccd.domain.service.cauroles.rolevalidator;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;

@Service
@Qualifier("default")
public class DefaultCaseAssignedUserRoleValidator implements CaseAssignedUserRoleValidator {

    private final String roleCaseWorkerCaa = "caseworker-caa";

    private final ApplicationParams applicationParams;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Autowired
    public DefaultCaseAssignedUserRoleValidator(ApplicationParams applicationParams,
                                                @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                                SecurityUtils securityUtils) {
        this.applicationParams = applicationParams;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    public boolean canAccessUserCaseRoles(List<String> userIds) {
        boolean canAccess = this.userRepository.getUserRoles().contains(roleCaseWorkerCaa);
        if (!canAccess) {
            userIds = userIds.stream().distinct().collect(Collectors.toList());
            canAccess = userIds.size() == 1 && userIds.contains(this.userRepository.getUserId());
        }
        return canAccess;
    }

    public boolean canAddUserCaseRoles() {
        return applicationParams.getAuthorisedServicesForAddUserCaseRoles().contains(securityUtils.getServiceName());
    }

}
