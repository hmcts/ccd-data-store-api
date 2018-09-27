package uk.gov.hmcts.ccd.data.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingInt;
import static org.apache.commons.lang.StringUtils.startsWithIgnoreCase;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.AuthCheckerConfiguration;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;

@Repository
@Qualifier(DefaultUserRepository.QUALIFIER)
public class DefaultUserRepository implements UserRepository {

    public static final String QUALIFIER = "default";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserRepository.class);
    private static final String RELEVANT_ROLES = "caseworker-%s";

    private final ApplicationParams applicationParams;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;

    @Autowired
    public DefaultUserRepository(ApplicationParams applicationParams,
                                 @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) CaseDefinitionRepository caseDefinitionRepository,
                                 SecurityUtils securityUtils,
                                 @Qualifier("restTemplate") RestTemplate restTemplate) {
        this.applicationParams = applicationParams;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
    }

    @Override
    public IDAMProperties getUserDetails() {
        final HttpEntity requestEntity = new HttpEntity(securityUtils.userAuthorizationHeaders());
        return restTemplate.exchange(applicationParams.idamUserProfileURL(), HttpMethod.GET, requestEntity, IDAMProperties.class).getBody();
    }

    @Override
    public Set<String> getUserRoles() {
        LOG.debug("retrieving user roles");
        final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return serviceAndUser.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<SecurityClassification> getUserClassifications(String jurisdictionId) {
        Set<String> roles = this.getUserRoles();
        final List<String> filteredRoles = roles.stream()
            .filter(role -> filterRole(jurisdictionId, role))
            .collect(Collectors.toList());

        return getClassificationsForUserRoles(filteredRoles);
    }

    /**
     * Gets user profile default settings.
     * @param userId user id
     * @return UserDefault
     */
    @Override
    public UserDefault getUserDefaultSettings(final String userId) {
        try {
            LOG.debug("retrieving default user settings for user {}", userId);
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            final Map<String, String> queryParams = new HashMap<>();
            queryParams.put("uid", userId);
            return restTemplate.exchange(applicationParams.userDefaultSettingsURL(),
                HttpMethod.GET, requestEntity, UserDefault.class, queryParams).getBody();
        } catch (HttpStatusCodeException e) {
            LOG.error("Failed to retrieve user profile", e);
            final List<String> headerMessages = e.getResponseHeaders().get("Message");
            final String message = headerMessages != null ? headerMessages.get(0) : e.getMessage();
            if (message != null) {
                throw new BadRequestException(message);
            }
            throw new ServiceException("Problem getting user default settings for " + userId);
        }
    }

    @Override
    public SecurityClassification getHighestUserClassification() {
        return getClassificationsForUserRoles(new ArrayList<>(getUserRoles()))
            .stream()
            .max(comparingInt(SecurityClassification::getRank))
            .orElseThrow(() -> new ServiceException("No security classification found for user"));
    }

    private Set<SecurityClassification> getClassificationsForUserRoles(List<String> roles) {
        return caseDefinitionRepository.getClassificationsForUserRoleList(roles).stream()
            .filter(Objects::nonNull)
            .filter(userRole -> Objects.nonNull(userRole.getSecurityClassification()))
            .map(userRole -> SecurityClassification.valueOf(userRole.getSecurityClassification()))
            .collect(Collectors.toSet());
    }

    private boolean filterRole(final String jurisdictionId, final String role) {
        return startsWithIgnoreCase(role, String.format(RELEVANT_ROLES, jurisdictionId))
            || ArrayUtils.contains(AuthCheckerConfiguration.getCitizenRoles(), role);
    }

}
