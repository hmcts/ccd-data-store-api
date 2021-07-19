package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
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
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "true")
@Lazy
public class DefaultCaseDataAccessControl implements CaseDataAccessControl, AccessControl {

    private final RoleAssignmentService roleAssignmentService;
    private final SecurityUtils securityUtils;
    private final RoleAssignmentsFilteringService roleAssignmentsFilteringService;
    private final AccessProfileService accessProfileService;
    private final PseudoRoleAssignmentsGenerator pseudoRoleAssignmentsGenerator;
    private final ApplicationParams applicationParams;
    private final PseudoRoleToAccessProfileGenerator pseudoRoleToAccessProfileGenerator;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDetailsRepository caseDetailsRepository;

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
                                                CaseDetailsRepository caseDetailsRepository) {
        this.roleAssignmentService = roleAssignmentService;
        this.securityUtils = securityUtils;
        this.roleAssignmentsFilteringService = roleAssignmentsFilteringService;
        this.pseudoRoleAssignmentsGenerator = pseudoRoleAssignmentsGenerator;
        this.applicationParams = applicationParams;
        this.accessProfileService = accessProfileService;
        this.pseudoRoleToAccessProfileGenerator = pseudoRoleToAccessProfileGenerator;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
    }

    // Returns List<AccessProfile>. Returns list of access profiles
    // for the user and filters access profiles based on the case type.
    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId) {
        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());
        List<RoleAssignment> filteredRoleAssignments = roleAssignmentsFilteringService
            .filter(roleAssignments, caseTypeDefinition).getFilteredMatchingRoleAssignments();

        return Sets.newHashSet(filteredAccessProfiles(filteredRoleAssignments, caseTypeDefinition));
    }

    @Override
    public Set<AccessProfile> generateAccessProfilesByCaseReference(String caseReference) {
        Optional<CaseDetails> caseDetails =  caseDetailsRepository.findByReference(caseReference);
        // R.A uses external micro-services which referer cases by caseReference
        // Non R.A uses internal case id. Both cases should be contemplated in the code.
        if (caseDetails.isEmpty()) {
            caseDetails = caseDetailsRepository.findById(null,Long.parseLong(caseReference));
            if (caseDetails.isEmpty()) {
                return Sets.newHashSet();
            }
        }
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());

        FilteredRoleAssignments filteredRoleAssignments =
            roleAssignmentsFilteringService.filter(roleAssignments, caseDetails.get());


        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseDetails.get().getCaseTypeId());

        return Sets.newHashSet(filteredAccessProfiles(filteredRoleAssignments.getFilteredMatchingRoleAssignments(),
            caseTypeDefinition));
    }

    private List<AccessProfile> filteredAccessProfiles(List<RoleAssignment> filteredRoleAssignments,
                                                       CaseTypeDefinition caseTypeDefinition) {
        if (applicationParams.getEnablePseudoRoleAssignmentsGeneration()) {
            List<RoleAssignment> pseudoRoleAssignments = pseudoRoleAssignmentsGenerator
                .createPseudoRoleAssignments(filteredRoleAssignments);
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
            pseudoAccessProfilesMappings.addAll(pseudoRoleToAccessProfileGenerator.generate(caseTypeDefinition));
        }
        return accessProfileService.generateAccessProfiles(filteredRoleAssignments, pseudoAccessProfilesMappings);
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
                .createPseudoRoleAssignments(filteringResults);
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
}
