package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProcess;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

@Component
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "true")
@Lazy
public class DefaultCaseDataAccessControl implements NoCacheCaseDataAccessControl, AccessControl {

    private final RoleAssignmentService roleAssignmentService;
    private final SecurityUtils securityUtils;
    private final RoleAssignmentsFilteringService roleAssignmentsFilteringService;
    private final AccessProfileService accessProfileService;
    private final PseudoRoleAssignmentsGenerator pseudoRoleAssignmentsGenerator;
    private final ApplicationParams applicationParams;
    private final PseudoRoleToAccessProfileGenerator pseudoRoleToAccessProfileGenerator;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final UserAuthorisation userAuthorisation;
    private final CaseUserRepository caseUserRepository;

    @Autowired
    public DefaultCaseDataAccessControl(RoleAssignmentService roleAssignmentService,
                                                SecurityUtils securityUtils,
                                                RoleAssignmentsFilteringService roleAssignmentsFilteringService,
                                                PseudoRoleAssignmentsGenerator pseudoRoleAssignmentsGenerator,
                                                ApplicationParams applicationParams,
                                                AccessProfileService accessProfileService,
                                                PseudoRoleToAccessProfileGenerator pseudoRoleToAccessProfileGenerator,
                                                @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                            final CaseDefinitionRepository caseDefinitionRepository,
                                                @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                                CaseDetailsRepository caseDetailsRepository,
                                                UserAuthorisation userAuthorisation,
                                        @Qualifier(CachedCaseUserRepository.QUALIFIER)
                                                CaseUserRepository caseUserRepository) {
        this.roleAssignmentService = roleAssignmentService;
        this.securityUtils = securityUtils;
        this.roleAssignmentsFilteringService = roleAssignmentsFilteringService;
        this.pseudoRoleAssignmentsGenerator = pseudoRoleAssignmentsGenerator;
        this.applicationParams = applicationParams;
        this.accessProfileService = accessProfileService;
        this.pseudoRoleToAccessProfileGenerator = pseudoRoleToAccessProfileGenerator;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.userAuthorisation = userAuthorisation;
        this.caseUserRepository = caseUserRepository;
    }

    // Returns List<AccessProfile>. Returns list of access profiles
    // for the user and filters access profiles based on the case type.
    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId) {
        return generateAllTypesOfProfilesByCaseTypeId(caseTypeId, false);
    }

    @Override
    public Set<AccessProfile> generateOrganisationalAccessProfilesByCaseTypeId(String caseTypeId) {
        return generateAllTypesOfProfilesByCaseTypeId(caseTypeId, true);
    }

    private Set<AccessProfile> generateAllTypesOfProfilesByCaseTypeId(String caseTypeId, boolean isCreationProfile) {
        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        final RoleAssignments roleAssignments;
        if (isCreationProfile) {
            roleAssignments = roleAssignmentService.getRoleAssignmentsForCreate(securityUtils.getUserId());
        } else {
            roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());
        }
        List<RoleAssignment> filteredRoleAssignments = roleAssignmentsFilteringService
            .filter(roleAssignments, caseTypeDefinition).getFilteredMatchingRoleAssignments();

        return Sets.newHashSet(filteredAccessProfiles(filteredRoleAssignments, caseTypeDefinition, isCreationProfile));
    }

    // use for ES searchCases - we don't want to fetch the case, because it should be taken from the ES
    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseDetails(CaseDetails caseDetails) {
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());

        FilteredRoleAssignments filteredRoleAssignments =
            roleAssignmentsFilteringService.filter(roleAssignments, caseDetails);

        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());

        return Sets.newHashSet(filteredAccessProfiles(filteredRoleAssignments.getFilteredMatchingRoleAssignments(),
            caseTypeDefinition, false));
    }

    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference) {
        Optional<CaseDetails> caseDetails =  caseDetailsRepository.findByReference(caseReference);
        // R.A uses external micro-services which referer cases by caseReference
        // Non R.A uses internal case id. Both cases should be contemplated in the code.
        if (caseDetails.isEmpty()) {
            caseDetails = caseDetailsRepository.findById(null, Long.parseLong(caseReference));
            if (caseDetails.isEmpty()) {
                return Sets.newHashSet();
            }
        }
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());

        FilteredRoleAssignments filteredRoleAssignments =
            roleAssignmentsFilteringService.filter(roleAssignments, caseDetails.get());


        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseDetails.get().getCaseTypeId());

        return Sets.newHashSet(filteredAccessProfiles(filteredRoleAssignments.getFilteredMatchingRoleAssignments(),
            caseTypeDefinition, false));
    }

    @Override
    public void grantAccess(CaseDetails caseDetails, String idamUserId) {
        if (UserAuthorisation.AccessLevel.GRANTED.equals(userAuthorisation.getAccessLevel())) {
            roleAssignmentService.createCaseRoleAssignments(caseDetails, idamUserId, Set.of(CREATOR.getRole()), false);
            if (applicationParams.getEnableCaseUsersDbSync()) {
                caseUserRepository.grantAccess(Long.valueOf(caseDetails.getId()), idamUserId, CREATOR.getRole());
            }
        }
    }

    public List<AccessProfile> filteredAccessProfiles(List<RoleAssignment> filteredRoleAssignments,
                                                       CaseTypeDefinition caseTypeDefinition,
                                                       boolean isCreationProfile) {
        if (applicationParams.getEnablePseudoRoleAssignmentsGeneration()) {
            List<RoleAssignment> pseudoRoleAssignments = pseudoRoleAssignmentsGenerator
                .createPseudoRoleAssignments(filteredRoleAssignments, isCreationProfile);
            filteredRoleAssignments = augment(filteredRoleAssignments, pseudoRoleAssignments);
        }

        if (hasGrantTypeExcludedRole(filteredRoleAssignments)) {
            filteredRoleAssignments = retainBasicAndSpecificGrantTypeRolesOnly(filteredRoleAssignments);
        }

        return generateAccessProfiles(filteredRoleAssignments, caseTypeDefinition);
    }


    private List<AccessProfile> generateAccessProfiles(List<RoleAssignment> filteredRoleAssignments,
                                                       CaseTypeDefinition caseTypeDefinition) {
        List<RoleToAccessProfileDefinition> pseudoAccessProfilesMappings = new ArrayList<>();
        pseudoAccessProfilesMappings.addAll(caseTypeDefinition.getRoleToAccessProfiles());
        if (applicationParams.getEnablePseudoAccessProfilesGeneration()) {
            List<RoleToAccessProfileDefinition> generated =
                pseudoRoleToAccessProfileGenerator.generate(caseTypeDefinition);
            pseudoAccessProfilesMappings.addAll(generated.stream()
                .filter(e -> getRoleNamesAsStream(caseTypeDefinition).noneMatch(p -> p.equals(e.getRoleName())))
                .collect(Collectors.toList()));
        }
        return accessProfileService.generateAccessProfiles(filteredRoleAssignments, pseudoAccessProfilesMappings);
    }

    private Stream<String> getRoleNamesAsStream(CaseTypeDefinition caseTypeDefinition) {
        return caseTypeDefinition.getRoleToAccessProfiles().stream().map(RoleToAccessProfileDefinition::getRoleName);
    }

    private List<RoleAssignment> augment(List<RoleAssignment> filteredRoleAssignments,
                                                  List<RoleAssignment> pseudoRoleAssignments) {
        pseudoRoleAssignments.addAll(filteredRoleAssignments);
        return pseudoRoleAssignments;
    }

    public boolean hasGrantTypeExcludedRole(List<RoleAssignment> roleAssignments) {
        return roleAssignments.stream()
            .anyMatch(roleAssignment -> roleAssignment.getGrantType().equals(GrantType.EXCLUDED.name()));
    }

    public List<RoleAssignment> retainBasicAndSpecificGrantTypeRolesOnly(List<RoleAssignment> roleAssignments) {
        return roleAssignments
            .stream()
            .filter(roleAssignment -> roleAssignment.getGrantType().equals(GrantType.BASIC.name())
                || roleAssignment.getGrantType().equals(GrantType.SPECIFIC.name()))
            .collect(Collectors.toList());
    }

    @Override
    public CaseAccessMetadata generateAccessMetadata(String caseId) {
        return createCaseAccessMetaDataByCaseId(caseId);
    }

    @Override
    public CaseAccessMetadata generateAccessMetadataWithNoCaseId() {
        CaseAccessMetadata caseAccessMetadata = new CaseAccessMetadata();
        caseAccessMetadata.setAccessGrants(List.of(GrantType.STANDARD));
        caseAccessMetadata.setAccessProcess(AccessProcess.NONE);

        return caseAccessMetadata;
    }

    private CaseAccessMetadata createCaseAccessMetaDataByCaseId(String caseId) {
        Optional<CaseDetails> caseDetails = caseDetailsRepository.findByReference(caseId);
        CaseAccessMetadata caseAccessMetadata = new CaseAccessMetadata();
        if (caseDetails.isPresent()) {
            FilteredRoleAssignments filteredRoleAssignments =
                roleAssignmentsFilteringService.filter(
                    roleAssignmentService.getRoleAssignments(securityUtils.getUserId()),
                    caseDetails.get());

            populateCaseAccessMetadata(caseAccessMetadata, filteredRoleAssignments);
        }
        return caseAccessMetadata;
    }

    @Override
    public boolean anyAccessProfileEqualsTo(String caseTypeId, String accessProfile) {
        Set<AccessProfile> accessProfiles =  generateAccessProfilesByCaseTypeId(caseTypeId);
        Set<String> accessProfileNames = AccessControlService.extractAccessProfileNames(accessProfiles);
        if (applicationParams.getEnablePseudoRoleAssignmentsGeneration()) {
            return accessProfileNames.contains(accessProfile)
                || accessProfileNames.contains(IDAM_PREFIX + accessProfile);
        }
        return accessProfileNames.contains(accessProfile);
    }

    private void populateCaseAccessMetadata(CaseAccessMetadata caseAccessMetadata,
                                            FilteredRoleAssignments filteredRoleAssignments) {
        List<RoleAssignment> pseudoRoleAssignments
            = appendGeneratedPseudoRoleAssignments(filteredRoleAssignments.getFilteredMatchingRoleAssignments());

        caseAccessMetadata.setAccessGrants(generatePostFilteringAccessGrants(pseudoRoleAssignments));
        caseAccessMetadata.setAccessProcess(generatePostFilteringAccessProcess(
            filteredRoleAssignments,
            pseudoRoleAssignments));
    }

    private List<RoleAssignment> appendGeneratedPseudoRoleAssignments(List<RoleAssignment> filteringResults) {
        if (applicationParams.getEnablePseudoRoleAssignmentsGeneration()) {
            List<RoleAssignment> pseudoRoleAssignments = pseudoRoleAssignmentsGenerator
                .createPseudoRoleAssignments(filteringResults, false);
            filteringResults = augment(filteringResults, pseudoRoleAssignments);
        }
        return filteringResults;
    }

    private AccessProcess generatePostFilteringAccessProcess(FilteredRoleAssignments filteredRoleAssignments,
                                                             List<RoleAssignment> pseudoGeneratedRoleAssignments) {
        boolean userHasAccessToCase = pseudoGeneratedRoleAssignments.stream()
            .map(roleAssignment -> GrantType.valueOf(roleAssignment.getGrantType()))
            .anyMatch(DefaultCaseDataAccessControl::isUserAllowedAccessToCase);

        if (userHasAccessToCase) {
            return AccessProcess.NONE;
        } else if (!filteredRoleAssignments.getFilteredRoleAssignmentsFailedOnRegionOrBaseLocationMatcher().isEmpty()) {
            return AccessProcess.CHALLENGED;
        } else {
            return AccessProcess.SPECIFIC;
        }
    }

    private static boolean isUserAllowedAccessToCase(GrantType grantType) {
        return grantType.equals(GrantType.STANDARD)
            || grantType.equals(GrantType.SPECIFIC)
            || grantType.equals(GrantType.CHALLENGED);
    }

    private List<GrantType> generatePostFilteringAccessGrants(List<RoleAssignment> roleAssignments) {
        return roleAssignments.stream()
            .map(roleAssignment -> GrantType.valueOf(roleAssignment.getGrantType()))
            .collect(Collectors.toList());
    }

    @Override
    public boolean shouldRemoveCaseDefinition(Set<AccessProfile> accessProfiles,
                                              Predicate<AccessControlList> access,
                                              String caseTypeId) {
        // In R.A if the access is create the RoleType has to be organisation.
        final var accessProfile = generateOrganisationalAccessProfilesByCaseTypeId(caseTypeId);
        return access.test(getCreateAccessControlList()) && accessProfile.isEmpty();
    }

    private AccessControlList getCreateAccessControlList() {
        var accessControlList = new AccessControlList();
        accessControlList.setCreate(true);
        return accessControlList;
    }
}
