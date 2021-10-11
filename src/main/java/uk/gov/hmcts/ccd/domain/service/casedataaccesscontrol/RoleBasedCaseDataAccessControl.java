package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

/**
 * Delete this class after we migrate CaseDataAccessControl fully.
 */
@Deprecated
@Component
@Lazy
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "false", matchIfMissing = true)
public class RoleBasedCaseDataAccessControl implements CaseDataAccessControl, AccessControl {
    private final UserAuthorisation userAuthorisation;
    private final CaseUserRepository caseUserRepository;
    private final UserRepository userRepository;

    @Autowired
    public RoleBasedCaseDataAccessControl(final UserAuthorisation userAuthorisation,
                                          final @Qualifier(CachedCaseUserRepository.QUALIFIER)
                                              CaseUserRepository caseUserRepository,
                                          @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {
        this.userAuthorisation = userAuthorisation;
        this.caseUserRepository = caseUserRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId) {
        return userRoleToAccessProfiles(userRepository.getUserRoles());
    }

    @Override
    public Set<AccessProfile> generateOrganisationalAccessProfilesByCaseTypeId(String caseTypeId) {
        return userRoleToAccessProfiles(userRepository.getUserRoles());
    }

    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference) {
        Set<String> roles = Sets.union(userRepository.getUserRoles(),
            Sets.newHashSet(caseUserRepository
                .findCaseRoles(Long.valueOf(caseReference), userRepository.getUserId())));
        return userRoleToAccessProfiles(roles);
    }

    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseDetails(CaseDetails caseDetails) {
        Set<String> roles = Sets.union(userRepository.getUserRoles(),
            Sets.newHashSet(caseUserRepository
                .findCaseRoles(caseDetails.getReference(), userRepository.getUserId())));
        return userRoleToAccessProfiles(roles);
    }

    @Override
    public void grantAccess(CaseDetails caseDetails, String idamUserId) {
        if (UserAuthorisation.AccessLevel.GRANTED.equals(userAuthorisation.getAccessLevel())) {
            caseUserRepository.grantAccess(Long.valueOf(caseDetails.getId()), idamUserId, CREATOR.getRole());
        }
    }

    @Override
    public Set<AccessProfile> getCaseUserAccessProfilesByUserId() {
        return userRoleToAccessProfiles(caseUserRepository.getCaseUserRolesByUserId(userRepository.getUserId()));
    }

    @Override
    public CaseAccessMetadata generateAccessMetadata(String reference) {
        return new CaseAccessMetadata();
    }

    @Override
    public boolean anyAccessProfileEqualsTo(String caseTypeId, String userRole) {
        return userRepository.anyRoleEqualsTo(userRole);
    }

    //Not applicable for enable-attribute-based-access-control=false.
    @Override
    public boolean shouldRemoveCaseDefinition(Set<AccessProfile> accessProfiles,
                                              Predicate<AccessControlList> access,
                                              String caseTypeId) {
        return false;
    }

    @Override
    public CaseAccessMetadata generateAccessMetadataWithNoCaseId() {
        return new CaseAccessMetadata();
    }

    private Set<AccessProfile> userRoleToAccessProfiles(Set<String> roles) {
        return roles
            .stream()
            .map(AccessProfile::new)
            .collect(Collectors.toSet());
    }


}
