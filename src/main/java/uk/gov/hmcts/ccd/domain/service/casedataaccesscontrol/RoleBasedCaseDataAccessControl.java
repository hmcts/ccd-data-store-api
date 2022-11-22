package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import static java.util.Collections.emptySet;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

/**
 * Delete this class after we migrate CaseDataAccessControl fully.
 */
@Deprecated
@Component
@Lazy
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "false", matchIfMissing = true)
public class RoleBasedCaseDataAccessControl implements NoCacheCaseDataAccessControl, AccessControl {
    private final UserAuthorisation userAuthorisation;
    private final CaseUserRepository caseUserRepository;
    private final UserRepository userRepository;
    private final CaseDetailsRepository caseDetailsRepository;


    @Autowired
    public RoleBasedCaseDataAccessControl(final UserAuthorisation userAuthorisation,
                                          final @Qualifier(CachedCaseUserRepository.QUALIFIER)
                                              CaseUserRepository caseUserRepository,
                                          @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                          final @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                              CaseDetailsRepository caseDetailsRepository
                                          ) {
        this.userAuthorisation = userAuthorisation;
        this.caseUserRepository = caseUserRepository;
        this.userRepository = userRepository;
        this.caseDetailsRepository = caseDetailsRepository;
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
                .findCaseRoles(Long.valueOf(getCaseId(caseReference)), userRepository.getUserId())));
        return userRoleToAccessProfiles(roles);
    }

    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseDetails(CaseDetails caseDetails) {
        Set<String> roles = Sets.union(userRepository.getUserRoles(),
            Sets.newHashSet(caseUserRepository
                .findCaseRoles(Long.valueOf(caseDetails.getId()), userRepository.getUserId())));
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

    @Override
    public Set<SecurityClassification> getUserClassifications(CaseTypeDefinition caseTypeDefinition,
                                                              boolean isCreateProfile) {
        return userRepository.getUserClassifications(caseTypeDefinition.getJurisdictionId());
    }

    @Override
    public Set<SecurityClassification> getUserClassifications(CaseDetails caseDetails) {
        return userRepository.getUserClassifications(caseDetails.getJurisdiction());
    }

    @Override
    public Set<AccessProfile> generateAccessProfilesForRestrictedCase(CaseDetails caseDetails) {
        // There's no such concept with old role-based access control
        return emptySet();
    }

    private Set<AccessProfile> userRoleToAccessProfiles(Set<String> roles) {
        return roles
            .stream()
            .map(AccessProfile::new)
            .collect(Collectors.toSet());
    }

    private String getCaseId(String caseReference) {
        Optional<CaseDetails> optionalCaseDetails =  caseDetailsRepository.findByReference(caseReference);
        // R.A uses external micro-services which referer cases by caseReference
        // Non R.A uses internal case id. Both cases should be contemplated in the code.
        if (optionalCaseDetails.isEmpty()) {
            optionalCaseDetails = caseDetailsRepository.findById(null,Long.parseLong(caseReference));
        }

        CaseDetails caseDetails = optionalCaseDetails.orElseThrow(() -> new CaseNotFoundException(caseReference));
        return caseDetails.getId();
    }


}
