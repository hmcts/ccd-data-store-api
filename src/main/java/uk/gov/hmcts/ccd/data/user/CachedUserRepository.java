package uk.gov.hmcts.ccd.data.user;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import static java.util.Comparator.comparingInt;

@Service
@RequestScope
@Qualifier(CachedUserRepository.QUALIFIER)
// TODO: Make this repository return copies of the maps https://tools.hmcts.net/jira/browse/RDM-1459
public class CachedUserRepository implements UserRepository {

    public static final String QUALIFIER = "cached";

    private final UserRepository userRepository;
    
    private Optional<String> userName = Optional.empty();

    @Autowired
    public CachedUserRepository(@Qualifier(DefaultUserRepository.QUALIFIER)
                                final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public IdamProperties getUserDetails() {
        return userRepository.getUserDetails();
    }

    @Override
    public IdamUser getUser() {
        return userRepository.getUser();
    }

    @Override
    public IdamUser getUser(String userToken) {
        return userRepository.getUser(userToken);
    }

    @Override
    public UserDefault getUserDefaultSettings(String userId) {
        return userRepository.getUserDefaultSettings(userId);
    }

    @Override
    public Set<String> getUserRoles() {
        return userRepository.getUserRoles();
    }

    @Override
    @Cacheable("userClassificationsByJurisdictionCache")
    public Set<SecurityClassification> getUserClassifications(String jurisdictionId) {
        return userRepository.getUserClassifications(jurisdictionId);
    }

    @Override
    @Cacheable("highestUserClassificationCache")
    public SecurityClassification getHighestUserClassification(String jurisdictionId) {
        return this.getUserClassifications(jurisdictionId)
            .stream()
            .max(comparingInt(SecurityClassification::getRank))
            .orElseThrow(() -> new ServiceException("No security classification found for user"));
    }

    @Override
    public String getUserId() {
        return userName.orElseGet(() -> {
            userName = Optional.of(userRepository.getUserId());
            return userName.get();
        });
    }

    @Override
    public List<String> getCaseworkerUserRolesJurisdictions() {
        return userRepository.getCaseworkerUserRolesJurisdictions();
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

    @Override
    public boolean isCrossJurisdictionRole(String role) {
        return userRepository.isCrossJurisdictionRole(role);
    }
}
