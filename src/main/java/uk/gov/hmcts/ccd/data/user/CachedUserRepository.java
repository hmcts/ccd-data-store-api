package uk.gov.hmcts.ccd.data.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;

@Service
@Qualifier(CachedUserRepository.QUALIFIER)
@RequestScope
// TODO: Make this repository return copies of the maps https://tools.hmcts.net/jira/browse/RDM-1459
public class CachedUserRepository implements UserRepository {

    public static final String QUALIFIER = "cached";

    private final UserRepository userRepository;
    private final Map<String, Set<SecurityClassification>> jurisdictionToUserClassifications = newHashMap();
    private final Map<String, IDAMProperties> userDetails = newHashMap();
    private final Map<String, Set<String>> userRoles = newHashMap();
    private final Map<String, UserProfile> userProfile = newHashMap();

    @Autowired
    public CachedUserRepository(@Qualifier(DefaultUserRepository.QUALIFIER) UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserProfile getUserSettings() {
        return userProfile.computeIfAbsent("userProfile", e -> userRepository.getUserSettings());
    }

    @Override
    public IDAMProperties getUserDetails() {
        return userDetails.computeIfAbsent("userDetails", e -> userRepository.getUserDetails());
    }

    @Override
    public Set<String> getUserRoles() {
        return userRoles.computeIfAbsent("userRoles", e -> userRepository.getUserRoles());
    }

    public Set<SecurityClassification> getUserClassifications(String jurisdictionId) {
        return jurisdictionToUserClassifications.computeIfAbsent(jurisdictionId, userRepository::getUserClassifications);
    }
}
