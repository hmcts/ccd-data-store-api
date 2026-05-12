package uk.gov.hmcts.ccd.domain.service.common;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.ProfessionalReferenceDataOrganisationRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

/**
 * Check access to a case for the current user.
 *
 * <p>User with the following roles should only be given access to the cases explicitly granted:
 * <ul>
 * <li>caseworker-*-solicitor: Solicitors</li>
 * <li>citizen(-loa[0-3]): Citizens</li>
 * <li>letter-holder: Citizen with temporary user account, as per CMC journey</li>
 * </ul>
 */
@Service
public class CaseAccessService {

    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;
    private final RoleAssignmentService roleAssignmentService;
    private final ApplicationParams applicationParams;
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseDataAccessControl caseDataAccessControl;
    private final ProfessionalReferenceDataOrganisationRepository professionalReferenceDataOrganisationRepository;
    private final CaseAccessGroupUtils caseAccessGroupUtils;

    private static final Pattern RESTRICT_GRANTED_ROLES_PATTERN
            = Pattern.compile(".+-solicitor$|.+-panelmember$|^citizen(-.*)?$|^letter-holder$|^caseworker-."
            + "+-localAuthority$");
    private static final Pattern ORG_BOUNDARY_RESTRICTED_ROLES_PATTERN
        = Pattern.compile(".+-solicitor$|.+-panelmember$|^caseworker-.+-localAuthority$");

    public CaseAccessService(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                             @Qualifier(CachedCaseUserRepository.QUALIFIER) CaseUserRepository caseUserRepository,
                             @Lazy CaseDataAccessControl caseDataAccessControl,
                             RoleAssignmentService roleAssignmentService, ApplicationParams applicationParams,
                             @Qualifier(CachedCaseUserRepository.QUALIFIER) CaseDetailsRepository caseDetailsRepository,
                             ProfessionalReferenceDataOrganisationRepository
                                 professionalReferenceDataOrganisationRepository,
                             CaseAccessGroupUtils caseAccessGroupUtils
    ) {

        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;
        this.roleAssignmentService = roleAssignmentService;
        this.applicationParams = applicationParams;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseDataAccessControl = caseDataAccessControl;
        this.professionalReferenceDataOrganisationRepository = professionalReferenceDataOrganisationRepository;
        this.caseAccessGroupUtils = caseAccessGroupUtils;
    }

    public Boolean canUserAccess(CaseDetails caseDetails) {
        if (userCanOnlyAccessExplicitlyGrantedCases()) {
            return isExplicitAccessGranted(caseDetails);
        } else {
            return true;
        }
    }

    public AccessLevel getAccessLevel(UserInfo userInfo) {
        return userInfo.getRoles()
            .stream()
            .filter(role -> RESTRICT_GRANTED_ROLES_PATTERN.matcher(role).matches())
            .findFirst()
            .map(role -> AccessLevel.GRANTED)
            .orElse(AccessLevel.ALL);
    }

    public Optional<List<Long>> getGrantedCaseReferencesForRestrictedRoles(CaseTypeDefinition caseTypeDefinition) {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            return getGrantedCaseReferences(caseTypeDefinition);
        } else {
            return getGrantedCaseReferences();
        }
    }

    private Optional<List<Long>> getGrantedCaseReferences(CaseTypeDefinition caseTypeDefinition) {
        final List<Long> caseReferences =
            roleAssignmentService
                .getCaseReferencesForAGivenUser(userRepository.getUserId(), caseTypeDefinition)
                .stream().map(Long::parseLong).collect(Collectors.toList());
        return Optional.of(caseReferences);
    }

    private Optional<List<Long>> getGrantedCaseReferences() {
        if (userCanOnlyAccessExplicitlyGrantedCases()) {
            final var ids = caseUserRepository.findCasesUserIdHasAccessTo(userRepository.getUserId());
            final var caseReferences = userCanOnlyAccessCasesWithinOrganisationBoundary()
                ? filterCaseReferencesByOrganisationBoundary(ids)
                : caseDetailsRepository.findCaseReferencesByIds(ids);
            return Optional.of(caseReferences);
        }
        return Optional.empty();
    }

    private List<Long> filterCaseReferencesByOrganisationBoundary(List<Long> caseIds) {
        final Optional<String> callerOrganisation =
            professionalReferenceDataOrganisationRepository.getCurrentUserOrganisationIdentifier();

        if (callerOrganisation.isEmpty()) {
            return List.of();
        }

        final List<Long> caseIdsWithinOrganisationBoundary = caseIds.stream()
            .map(caseId -> caseDetailsRepository.findById(null, caseId).orElse(null))
            .filter(caseDetails -> caseDetails != null && isCaseWithinCallerOrganisation(caseDetails,
                callerOrganisation.get()))
            .map(caseDetails -> Long.valueOf(caseDetails.getId()))
            .collect(Collectors.toList());

        return caseDetailsRepository.findCaseReferencesByIds(caseIdsWithinOrganisationBoundary);
    }

    private boolean isCaseWithinCallerOrganisation(CaseDetails caseDetails, String callerOrganisation) {
        return caseUserRepository.findCaseRoles(Long.valueOf(caseDetails.getId()), userRepository.getUserId()).stream()
            .map(caseRole -> caseAccessGroupUtils.findOrganisationPolicyNodeForCaseRole(caseDetails, caseRole))
            .filter(node -> node != null && node.get("Organisation") != null
                && node.get("Organisation").get("OrganisationID") != null)
            .map(node -> node.get("Organisation").get("OrganisationID").asText())
            .anyMatch(callerOrganisation::equals);
    }

    public Set<String> getCaseRoles(String caseId) {
        return new HashSet<>(caseUserRepository.findCaseRoles(Long.valueOf(caseId), userRepository.getUserId()));
    }

    public Set<AccessProfile> getAccessProfilesByCaseReference(String caseReference) {
        return caseDataAccessControl.generateAccessProfilesByCaseReference(caseReference);
    }

    public Set<AccessProfile> getCaseCreationCaseRoles() {
        return Collections.singleton(AccessProfile.builder()
            .readOnly(false)
            .accessProfile(CREATOR.getRole()).build());
    }

    public Set<AccessProfile> getCaseCreationRoles(String caseTypeId) {
        return Sets.union(getCreationAccessProfiles(caseTypeId), getCaseCreationCaseRoles());
    }

    public Set<AccessProfile> getAccessProfiles(String caseTypeId) {
        Set<AccessProfile> accessProfiles = caseDataAccessControl.generateAccessProfilesByCaseTypeId(caseTypeId);
        if (accessProfiles == null) {
            throw new ValidationException("Cannot find access profiles for the user");
        }
        return accessProfiles;
    }

    public Set<AccessProfile> getCreationAccessProfiles(String caseTypeId) {
        Set<AccessProfile> accessProfiles =
            caseDataAccessControl.generateOrganisationalAccessProfilesByCaseTypeId(caseTypeId);
        if (accessProfiles == null) {
            throw new ValidationException("Cannot find access profiles for the user");
        }
        return accessProfiles;
    }

    public boolean isJurisdictionAccessAllowed(String jurisdiction) {
        return this.userRepository
            .getCaseworkerUserRolesJurisdictions()
            .stream()
            .anyMatch(jurisdiction::equalsIgnoreCase);
    }


    public Boolean isExplicitAccessGranted(CaseDetails caseDetails) {
        final List<Long> grantedCases = caseUserRepository.findCasesUserIdHasAccessTo(userRepository.getUserId());

        if (null != grantedCases && grantedCases.contains(Long.valueOf(caseDetails.getId()))) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    public Boolean isExternalUser() {
        return userCanOnlyAccessExplicitlyGrantedCases();
    }

    public Boolean userCanOnlyAccessExplicitlyGrantedCases() {
        return userRepository.anyRoleMatches(RESTRICT_GRANTED_ROLES_PATTERN);
    }

    public Boolean userCanOnlyAccessCasesWithinOrganisationBoundary() {
        return userRepository.anyRoleMatches(ORG_BOUNDARY_RESTRICTED_ROLES_PATTERN);
    }
}
