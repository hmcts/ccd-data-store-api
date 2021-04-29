package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleMatchingResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

@Component
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "true")
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
                                        @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
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

    // Returns Optional<CaseDetails>. If this is not enough think of wrapping it in a AccessControlResponse
    // that contains the additional information about the operation result
    @Override
    public List<AccessProfile> generateAccessProfilesByCaseTypeId(String caseTypeId) {
        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());
        RoleAssignmentFilteringResult filteringResults = roleAssignmentsFilteringService
            .filter(roleAssignments, caseTypeDefinition);

        return filteredAccessProfiles(filteringResults, caseTypeDefinition);
    }

    @Override
    public List<AccessProfile> generateAccessProfilesByCaseReference(String caseReference) {
        Optional<CaseDetails> caseDetails =  caseDetailsRepository.findByReference(caseReference);
        if (caseDetails.isEmpty()) {
            return Lists.newArrayList();
        }
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());
        RoleAssignmentFilteringResult filteringResults = roleAssignmentsFilteringService
            .filter(roleAssignments, caseDetails.get());
        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseDetails.get().getCaseTypeId());

        return filteredAccessProfiles(filteringResults, caseTypeDefinition);
    }

    private List<AccessProfile> filteredAccessProfiles(RoleAssignmentFilteringResult filteringResults,
                                                       CaseTypeDefinition caseTypeDefinition) {
        if (applicationParams.getEnablePseudoRoleAssignmentsGeneration()) {
            List<RoleAssignment> pseudoRoleAssignments = pseudoRoleAssignmentsGenerator
                .createPseudoRoleAssignments(filteringResults);
            filteringResults = augment(filteringResults, pseudoRoleAssignments);
        }

        if (filteringResults.hasGrantTypeExcludedRole()) {
            filteringResults = filteringResults.retainBasicAndSpecificGrantTypeRolesOnly();
        }

        return generateAccessProfiles(filteringResults, caseTypeDefinition);
    }


    private List<AccessProfile> generateAccessProfiles(RoleAssignmentFilteringResult filteringResults,
                                                       CaseTypeDefinition caseTypeDefinition) {
        if (applicationParams.getEnablePseudoAccessProfilesGeneration()) {
            List<RoleToAccessProfileDefinition> pseudoAccessProfilesMappings =
                pseudoRoleToAccessProfileGenerator.generate(caseTypeDefinition);

            pseudoAccessProfilesMappings.addAll(caseTypeDefinition.getRoleToAccessProfiles());

            return accessProfileService
                .generateAccessProfiles(filteringResults, pseudoAccessProfilesMappings);
        } else {
            return accessProfileService
                .generateAccessProfiles(filteringResults, caseTypeDefinition.getRoleToAccessProfiles());
        }
    }

    private RoleAssignmentFilteringResult augment(RoleAssignmentFilteringResult filteringResults,
                                                  List<RoleAssignment> pseudoRoleAssignments) {
        List<Pair<RoleAssignment, RoleMatchingResult>> augmented = pseudoRoleAssignments
            .stream()
            .map(roleAssignment -> Pair.of(roleAssignment, new RoleMatchingResult()))
            .collect(Collectors.toList());

        augmented.addAll(filteringResults.getRoleMatchingResults());
        return new RoleAssignmentFilteringResult(augmented);
    }

    @Override
    public void grantAccess(String caseId, String idamUserId) {

    }
}
