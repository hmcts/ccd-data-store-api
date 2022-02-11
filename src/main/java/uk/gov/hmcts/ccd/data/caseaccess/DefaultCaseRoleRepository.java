package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpMethod.GET;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseRoleDefinition;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import static org.springframework.http.HttpMethod.GET;

@Service
@Qualifier(DefaultCaseRoleRepository.QUALIFIER)
public class DefaultCaseRoleRepository implements CaseRoleRepository {

    public static final String QUALIFIER = "default";
    public static final String DEFAULT_USER_ID = "uid";
    public static final String DEFAULT_JURISDICTION_ID = "jid";

    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;

    @Qualifier("restTemplate")
    @Autowired
    private final RestTemplate restTemplate;

    @Inject
    public DefaultCaseRoleRepository(ApplicationParams applicationParams,
                                     SecurityUtils securityUtils,
                                     @Qualifier("restTemplate") final RestTemplate restTemplate) {
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
    }

    @Override
    @Cacheable(value = "caseRolesCache", unless = "#result == null or #result.size() == 0")
    public Set<String> getCaseRoles(String caseTypeId) {
        return getCaseRoles(DEFAULT_USER_ID, DEFAULT_JURISDICTION_ID, caseTypeId);
    }

    @Override
    public Set<String> getCaseRoles(String userId, String jurisdictionId, String caseTypeId) {
        final HttpEntity<CaseRoleDefinition[]> requestEntity = new HttpEntity<>(securityUtils.authorizationHeaders());
        CaseRoleDefinition[] caseRoleDefinitions =
            restTemplate.exchange(applicationParams.caseRolesURL(userId, jurisdictionId, caseTypeId), GET,
                requestEntity, CaseRoleDefinition[].class).getBody();

        return (caseRoleDefinitions == null)
            ? new HashSet<>()
            : Arrays.stream(caseRoleDefinitions).map(CaseRoleDefinition::getId).collect(Collectors.toSet());
    }

    private Set<String> getAccessProfilesRoles(String caseTypeId) {
        final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
        final var caseRolesUrl = applicationParams.accessProfileRolesURL(caseTypeId);
        CaseRoleDefinition[] caseRoleDefinitions =
            restTemplate.exchange(caseRolesUrl, GET, requestEntity, CaseRoleDefinition[].class).getBody();
        return Arrays.stream(caseRoleDefinitions).map(CaseRoleDefinition::getName).collect(Collectors.toSet());
    }

    public Set<String> getRoles(String caseTypeId) {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            final var accessProfilesRoles = this.getAccessProfilesRoles(caseTypeId);
            return accessProfilesRoles.isEmpty() ? this.getCaseRoles(caseTypeId) : accessProfilesRoles;
        } else {
            return this.getCaseRoles(caseTypeId);
        }
    }
}
