package uk.gov.hmcts.ccd.domain.service.cauroles.rolevalidator;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;

@Service
@Qualifier("default")
public class DefaultCaseAssignedUserRoleValidator implements CaseAssignedUserRoleValidator {


    @Value("#{'${casedatastore.authorised.services.add_caseassigned_user_roles}'.split(',')}")
    private List<String> authorisedAddServices;

    private final String roleCaseWorkerCaa = "caseworker-caa";

    private UserRepository userRepository;
    private SecurityUtils securityUtils;

    @Autowired
    public DefaultCaseAssignedUserRoleValidator(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                                SecurityUtils securityUtils) {
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
        return authorisedAddServices.contains(securityUtils.getServiceName());
    }

}
