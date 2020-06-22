package uk.gov.hmcts.ccd.domain.service.supplementarydata.rolevalidator;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

@Service
@Qualifier("default")
public class DefaultSupplementaryDataUserRoleValidator implements SupplementaryDataUserRoleValidator {

    private final String roleCaseWorkerCaa = "caseworker-caa";

    private final UserRepository userRepository;
    private final CaseAccessService caseAccessService;

    @Autowired
    public DefaultSupplementaryDataUserRoleValidator(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                                     final CaseAccessService caseAccessService) {
        this.userRepository = userRepository;
        this.caseAccessService = caseAccessService;
    }

    public boolean canUpdateSupplementaryData(List<String> userIds) {
        boolean canAccess = this.userRepository.getUserRoles().contains(roleCaseWorkerCaa);
        if (!canAccess) {
            userIds = userIds.stream().distinct().collect(Collectors.toList());
            canAccess = userIds.size() == 1 && userIds.contains(this.userRepository.getUserId());
        }

        return canAccess;
    }
}
