package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.mapper.RoleAssignmentToAccessProfileMapper;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

@Component
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "true")
public class DefaultCaseDataAccessControl implements CaseDataAccessControl, AccessControl {

    private final RoleAssignmentService roleAssignmentService;
    private final SecurityUtils securityUtils;
    private final CaseService caseService;
    private final CaseTypeService caseTypeService;
    private final RoleAssignmentsFilteringService roleAssignmentsFilteringService;
    private final RoleAssignmentToAccessProfileMapper roleAssignmentToAccessProfileMapper;
    private final FakeRoleAssignmentsGenerator fakeRoleAssignmentsGenerator;
    private final ApplicationParams applicationParams;
    private final RoleToAccessProfilesMappingsGenerator roleToAccessProfilesMappingsGenerator;

    @Autowired
    public DefaultCaseDataAccessControl(RoleAssignmentService roleAssignmentService,
                                        SecurityUtils securityUtils,
                                        CaseService caseService,
                                        CaseTypeService caseTypeService,
                                        RoleAssignmentsFilteringService roleAssignmentsFilteringService,
                                        FakeRoleAssignmentsGenerator fakeRoleAssignmentsGenerator,
                                        ApplicationParams applicationParams,
                                        RoleAssignmentToAccessProfileMapper roleAssignmentToAccessProfileMapper,
                                        RoleToAccessProfilesMappingsGenerator roleToAccessProfilesMappingsGenerator) {
        this.roleAssignmentService = roleAssignmentService;
        this.securityUtils = securityUtils;
        this.caseService = caseService;
        this.caseTypeService = caseTypeService;
        this.roleAssignmentsFilteringService = roleAssignmentsFilteringService;
        this.fakeRoleAssignmentsGenerator = fakeRoleAssignmentsGenerator;
        this.applicationParams = applicationParams;
        this.roleAssignmentToAccessProfileMapper = roleAssignmentToAccessProfileMapper;
        this.roleToAccessProfilesMappingsGenerator = roleToAccessProfilesMappingsGenerator;
    }

    // Returns Optional<CaseDetails>. If this is not enough think of wrapping it in a AccessControlResponse
    // that contains the additional information about the operation result
    @Override
    public Optional<CaseDetails> applyAccessControl(CaseDetails caseDetails) {
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(securityUtils.getUserId());
        CaseDetails cloned = caseService.clone(caseDetails);

        RoleAssignmentFilteringResult filteringResults = roleAssignmentsFilteringService
            .filter(roleAssignments, caseDetails);
        CaseTypeDefinition caseTypeDefinition = caseTypeService.getCaseType(caseDetails.getCaseTypeId());

        if (applicationParams.getEnablePseudoRoleAssignmentsGeneration()) {
            List<RoleAssignment> augmentedRoleAssignments = fakeRoleAssignmentsGenerator
                .addFakeRoleAssignments(filteringResults);
        }

        List<AccessProfile> accessProfiles = roleAssignmentToAccessProfileMapper
            .toAccessProfiles(filteringResults, caseTypeDefinition);

        List<AccessProfile> generatedAccessProfiles = roleToAccessProfilesMappingsGenerator.generate(caseTypeDefinition);
        // 4.) determine AccessProfiles from the new RoleToAccessProfiles Tab
        // https://tools.hmcts.net/confluence/pages/viewpage.action?pageId=1460559903#AccessControlScopeofDelivery-NewRoleToAccessProfilesTab
        // as a result we identify the AccessProfiles that the user has on the case

        return Optional.of(cloned);
    }

    @Override
    public void grantAccess(String caseId, String idamUserId) {

    }
}
