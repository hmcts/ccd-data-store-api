package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;

@Service
public class CaseRoleService {
    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;

    public CaseRoleService(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                           CaseUserRepository caseUserRepository) {
        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;

    }

    public Set<String> getCaseRoles(String caseId) {
        return caseUserRepository
            .findCaseRoles(Long.valueOf(caseId), userRepository.getUserId())
            .stream()
            .collect(Collectors.toSet());
    }
}
