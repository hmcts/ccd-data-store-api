package uk.gov.hmcts.ccd.data.user;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.collect.Maps.newHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;

@Service
@Qualifier(CachedUserRepository.QUALIFIER)
@RequestScope
// TODO: Make this repository return copies of the maps https://tools.hmcts.net/jira/browse/RDM-1459
public class CachedUserRepository implements UserRepository {

    public static final String QUALIFIER = "cached";

    private final UserRepository userRepository;
    private final Map<String, Set<SecurityClassification>> jurisdictionToUserClassifications = newHashMap();
    private final Map<String, IdamProperties> userDetails = newHashMap();
    private final Map<String, Set<String>> userRoles = newHashMap();
    private final Map<String, SecurityClassification> userHighestSecurityClassification = newHashMap();
    private Optional<String> userName = Optional.empty();

    @Autowired
    public CachedUserRepository(@Qualifier(DefaultUserRepository.QUALIFIER) UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public IdamProperties getUserDetails() {
        return userDetails.computeIfAbsent("userDetails", e -> userRepository.getUserDetails());
    }

    @Override
    public IdamUser getUser() {
        return userRepository.getUser();
    }

    @Override
    public UserDefault getUserDefaultSettings(String userId) {
        return userRepository.getUserDefaultSettings(userId);
    }

    @Override
    public Set<String> getUserRoles() {
        return userRoles.computeIfAbsent("userRoles", e -> userRepository.getUserRoles());
    }

    @Override
    public Set<SecurityClassification> getUserClassifications(String jurisdictionId) {
        return jurisdictionToUserClassifications.computeIfAbsent(jurisdictionId, userRepository::getUserClassifications);
    }

    @Override
    public SecurityClassification getHighestUserClassification(String jurisdictionId) {
        return userHighestSecurityClassification.computeIfAbsent(jurisdictionId, s -> userRepository.getHighestUserClassification(jurisdictionId));
    }

    @Override
    public String getUserId() {
        return userName.orElseGet(() -> {
            userName = Optional.of(userRepository.getUserId());
            return userName.get();
        });
    }

    @Override
    public List<String> getUserRolesJurisdictions() {
        return userRepository.getUserRolesJurisdictions();
    }

    @Override
    public boolean anyRoleEqualsAnyOf(List<String> userRoles) {
        return userRepository.anyRoleEqualsAnyOf(userRoles);
    }

    @Override
    public boolean anyRoleEqualsTo(String userRole) {
        return userRepository.anyRoleEqualsTo(userRole);
    }

    @Override
    public boolean anyRoleMatches(Pattern rolesPattern) {
        return userRepository.anyRoleMatches(rolesPattern);
    }
}
