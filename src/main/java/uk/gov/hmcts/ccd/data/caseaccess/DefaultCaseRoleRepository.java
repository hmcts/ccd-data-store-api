package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpMethod.GET;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseRoleDefinition;

@Service
@Qualifier(DefaultCaseRoleRepository.QUALIFIER)
public class DefaultCaseRoleRepository implements CaseRoleRepository {

    public static final String QUALIFIER = "default";

    private final ApplicationParams applicationParams;
    private final RestTemplate restTemplate;
    private final SecurityUtils securityUtils;

    public DefaultCaseRoleRepository(ApplicationParams applicationParams,
                                     SecurityUtils securityUtils,
                                     @Qualifier("restTemplate") final RestTemplate restTemplate) {
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
    }

    @Override
    public Set<String> getCaseRoles(String caseTypeId) {
        final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
        String caseRolesUrl = String.format("%s/%s/roles", applicationParams.caseRolesURL(), caseTypeId);
        CaseRoleDefinition[] caseRoleDefinitions = restTemplate.exchange(caseRolesUrl, GET, requestEntity, CaseRoleDefinition[].class).getBody();

        return Arrays.stream(caseRoleDefinitions).map(CaseRoleDefinition::getId).collect(Collectors.toSet());
    }
}
