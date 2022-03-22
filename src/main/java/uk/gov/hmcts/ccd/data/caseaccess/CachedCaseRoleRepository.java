package uk.gov.hmcts.ccd.data.caseaccess;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;

@Service
@Qualifier(CachedCaseRoleRepository.QUALIFIER)
@RequestScope
public class CachedCaseRoleRepository implements CaseRoleRepository {

    public static final String QUALIFIER = "cached";

    private final CaseRoleRepository caseRoleRepository;
    private final Map<String, Set<String>> caseRoles = newHashMap();
    private final Map<String, Set<String>> roles = newHashMap();

    public CachedCaseRoleRepository(@Qualifier(DefaultCaseRoleRepository.QUALIFIER)
                                    final CaseRoleRepository caseRoleRepository) {
        this.caseRoleRepository = caseRoleRepository;
    }

    @Override
    public Set<String> getCaseRoles(String caseTypeId) {
        return caseRoles.computeIfAbsent(caseTypeId, caseRoleRepository::getCaseRoles);
    }

    @Override
    public Set<String> getCaseRoles(String userId, String jurisdictionId, String caseTypeId) {
        return getCaseRoles(caseTypeId);
    }

    @Override
    public Set<String> getRoles(String caseTypeId) {
        return roles.computeIfAbsent(caseTypeId, caseRoleRepository::getRoles);
    }
}
