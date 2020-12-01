package uk.gov.hmcts.ccd.data.user;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingInt;
import static java.util.function.Predicate.not;
import static org.apache.commons.lang.StringUtils.startsWithIgnoreCase;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.AuthCheckerConfiguration;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

@Repository
@Qualifier(DefaultUserRepository.QUALIFIER)
public class DefaultUserRepository implements UserRepository {

    public static final String QUALIFIER = "default";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserRepository.class);
    private static final String RELEVANT_ROLES = "caseworker-%s";
    private static final int JURISDICTION_INDEX = 1;

    private final ApplicationParams applicationParams;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;
    private final AuthCheckerConfiguration authCheckerConfiguration;

    @Autowired
    public DefaultUserRepository(ApplicationParams applicationParams,
                                 @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                     CaseDefinitionRepository caseDefinitionRepository,
                                 SecurityUtils securityUtils,
                                 @Qualifier("restTemplate") RestTemplate restTemplate,
                                 AuthCheckerConfiguration authCheckerConfiguration) {
        this.applicationParams = applicationParams;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
        this.authCheckerConfiguration = authCheckerConfiguration;
    }

    @Override
    public IdamProperties getUserDetails() {
        UserInfo userInfo = securityUtils.getUserInfo();
        return  toIdamProperties(userInfo);
    }

    @Override
    public IdamUser getUser() {
        UserInfo userInfo = securityUtils.getUserInfo();
        return toIdamUser(userInfo);
    }

    @Override
    public Set<String> getUserRoles() {
        LOG.debug("Getting user roles from security context.");

        Collection<? extends GrantedAuthority> authorities =
            SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        Set<String> userRoles = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        String userId = getUser().getId();
        LOG.info("User id from idam: {}. User roles in the security context: {}.", userId, userRoles);

        return userRoles;
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
            final HttpEntity requestEntity = new HttpEntity(securityUtils.authorizationHeaders());
            final Map<String, String> queryParams = new HashMap<>();
            queryParams.put("uid", ApplicationParams.encode(userId.toLowerCase()));
            final String encodedUrl = UriComponentsBuilder.fromHttpUrl(applicationParams.userDefaultSettingsURL())
                .buildAndExpand(queryParams).toUriString();
            return restTemplate.exchange(new URI(encodedUrl), HttpMethod.GET, requestEntity, UserDefault.class)
                .getBody();
        } catch (RestClientResponseException e) {
            LOG.error("Failed to retrieve user profile", e);
            final List<String> headerMessages = Optional.ofNullable(e.getResponseHeaders())
                .map(headers -> headers.get("Message")).orElse(null);
            final String message = headerMessages != null ? headerMessages.get(0) : e.getMessage();
            if (message != null) {
                if (HttpStatus.NOT_FOUND.value() == e.getRawStatusCode()) {
                    throw new ResourceNotFoundException(message);
                } else {
                    throw new BadRequestException(message);
                }
            }
            throw new ServiceException("Problem getting user default settings for " + userId);
        } catch (ResourceAccessException e) {
            LOG.error("Failed to retrieve user profile - I/O error", e);
            throw new ServiceException("Problem getting user default settings for " + userId);
        } catch (URISyntaxException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    public SecurityClassification getHighestUserClassification(String jurisdictionId) {
        return getUserClassifications(jurisdictionId)
            .stream()
            .max(comparingInt(SecurityClassification::getRank))
            .orElseThrow(() -> new ServiceException("No security classification found for user"));
    }

    @Override
    public String getUserId() {
        return securityUtils.getUserId();
    }

    @Override
    public List<String> getCaseworkerUserRolesJurisdictions() {
        String[] roles = this.getUserDetails().getRoles();

        return Arrays.stream(roles)
            .filter(this::isCaseworkerRole)
            .filter(not(this::isCrossJurisdictionRole))
            .map(this::extractJurisdiction)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public boolean anyRoleEqualsAnyOf(List<String> userRoles) {
        return getUserRoles().stream().anyMatch(userRoles::contains);
    }

    @Override
    public boolean anyRoleEqualsTo(String userRole) {
        return getUserRoles().contains(userRole);
    }

    @Override
    public boolean anyRoleMatches(Pattern rolesPattern) {
        return getUserRoles().stream().anyMatch(role -> rolesPattern.matcher(role).matches());
    }

    @Override
    public boolean isCrossJurisdictionRole(String role) {
        return applicationParams.getCcdAccessControlCrossJurisdictionRoles().contains(role);
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
                || ArrayUtils.contains(authCheckerConfiguration.getCitizenRoles(), role)
                || isCrossJurisdictionRole(role);
    }

    private IdamProperties toIdamProperties(UserInfo userInfo) {
        IdamProperties idamProperties = new IdamProperties();
        idamProperties.setId(userInfo.getUid());
        idamProperties.setEmail(userInfo.getSub());
        idamProperties.setForename(userInfo.getGivenName());
        idamProperties.setSurname(userInfo.getFamilyName());
        idamProperties.setRoles(userInfo.getRoles().toArray(new String[0]));
        return idamProperties;
    }

    private IdamUser toIdamUser(UserInfo userInfo) {
        IdamUser idamUser = new IdamUser();
        idamUser.setId(userInfo.getUid());
        idamUser.setEmail(userInfo.getSub());
        idamUser.setForename(userInfo.getGivenName());
        idamUser.setSurname(userInfo.getFamilyName());
        return idamUser;
    }

    private Optional<String> extractJurisdiction(String caseworkerRole) {
        String[] parts = caseworkerRole.split("-");
        return parts.length < 2 ? Optional.empty() : Optional.of(parts[JURISDICTION_INDEX]);
    }

    private boolean isCaseworkerRole(String role) {
        return role.matches(applicationParams.getCcdAccessControlCaseworkerRoleRegex());
    }
}
