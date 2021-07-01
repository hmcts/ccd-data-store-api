package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProcess;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.AuthorisationMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType.BASIC;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType.CHALLENGED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType.SPECIFIC;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType.STANDARD;

class DefaultCaseDataAccessControlTest {

    private static final String ROLE_NAME_1 = "TEST_ROLE_1";
    private static final String ROLE_NAME_2 = "TEST_ROLE_2";
    private static final String ROLE_NAME_3 = "TEST_ROLE_3";
    private static final String ROLE_NAME_4 = "TEST_ROLE_4";
    private static final String ROLE_NAME_5 = "TEST_ROLE_5";
    private static final String ACTOR_ID_1 = "ACTOR_ID_1";
    private static final String USER_ID = "USER_ID";

    private static final String AUTHORISATION_1 = "TEST_AUTH_1";
    private static final String AUTHORISATION_2 = "TEST_AUTH_2";

    private static final String CASE_TYPE_1 = "TEST_CASE_TYPE";

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RoleAssignmentsFilteringService roleAssignmentsFilteringService;

    private AccessProfileServiceImpl accessProfileService;

    @Mock
    private PseudoRoleAssignmentsGenerator pseudoRoleAssignmentsGenerator;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private PseudoRoleToAccessProfileGenerator pseudoRoleToAccessProfileGenerator;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    private DefaultCaseDataAccessControl defaultCaseDataAccessControl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        accessProfileService = spy(new AccessProfileServiceImpl(new AuthorisationMapper()));
        defaultCaseDataAccessControl = new DefaultCaseDataAccessControl(roleAssignmentService, securityUtils,
            roleAssignmentsFilteringService, pseudoRoleAssignmentsGenerator, applicationParams,
            accessProfileService, pseudoRoleToAccessProfileGenerator, caseDefinitionRepository, caseDetailsRepository);
    }

    @Test
    void generateAccessProfilesByCaseTypeId() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, BASIC.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);
        doReturn(roleAssignments1).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        doReturn(false).when(applicationParams).getEnablePseudoAccessProfilesGeneration();

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseTypeId(CASE_TYPE_1);

        assertNotNull(accessProfiles);
        assertEquals(1, accessProfiles.size());
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService).filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(applicationParams).getEnablePseudoAccessProfilesGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldReturnEmptyAccessProfilesWhenAccessProfilesHasExcludeGrantType() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, GrantType.EXCLUDED.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);
        doReturn(roleAssignments1).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        doReturn(false).when(applicationParams).getEnablePseudoAccessProfilesGeneration();

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseTypeId(CASE_TYPE_1);

        assertNotNull(accessProfiles);
        assertEquals(0, accessProfiles.size());
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(applicationParams).getEnablePseudoAccessProfilesGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }


    @Test
    void shouldReturnAccessProfilesWhenAccessProfilesHasExcludeGrantTypeAndOtherGrantTypes() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1, ROLE_NAME_4);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, BASIC.name());
        roleAndGrantType.put(ROLE_NAME_2, GrantType.EXCLUDED.name());
        roleAndGrantType.put(ROLE_NAME_3, CHALLENGED.name());
        roleAndGrantType.put(ROLE_NAME_4, SPECIFIC.name());
        roleAndGrantType.put(ROLE_NAME_5, GrantType.STANDARD.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);

        doReturn(roleAssignments1).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        doReturn(false).when(applicationParams).getEnablePseudoAccessProfilesGeneration();

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseTypeId(CASE_TYPE_1);

        assertNotNull(accessProfiles);
        assertEquals(2, accessProfiles.size());
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(applicationParams).getEnablePseudoAccessProfilesGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    private CaseTypeDefinition createCaseTypeDefinition(String... roleNames) {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setAccessControlLists(createAccessControlList());
        caseTypeDefinition.setRoleToAccessProfiles(createRoleToAccessProfileDefinitions(roleNames));
        return caseTypeDefinition;
    }

    private List<RoleToAccessProfileDefinition> createRoleToAccessProfileDefinitions(String... roleNames) {
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = Lists.newArrayList();

        Arrays.stream(roleNames).forEach(roleName -> {
            roleToAccessProfileDefinitions.add(
                new RoleToAccessProfileDefinition(
                    CASE_TYPE_1, false, false, AUTHORISATION_1,
                    roleName, null, null, roleName
                ));
        });

        return roleToAccessProfileDefinitions;
    }

    @Test
    void testGetCaseUserAccessProfilesByUserId() {
        Set<AccessProfile>  accessProfiles = defaultCaseDataAccessControl.getCaseUserAccessProfilesByUserId();

        assertNotNull(accessProfiles);
        assertEquals(0, accessProfiles.size());
    }

    @Test
    void testGrantAccess() {
        defaultCaseDataAccessControl.grantAccess(null, USER_ID);
    }

    private List<AccessControlList> createAccessControlList() {
        AccessControlList accessControlList = new AccessControlList();
        accessControlList.setAccessProfile(ROLE_NAME_1);

        return Lists.newArrayList(accessControlList);
    }

    private List<RoleAssignment> createFilteringResults(Map<String, String> roleNameAndGrantType) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        roleNameAndGrantType.entrySet()
            .stream()
            .forEach(entry -> roleAssignments
                .add(createRoleAssignmentAndRoleMatchingResult(entry.getKey(), entry.getValue())));

        return roleAssignments;
    }

    private RoleAssignment createRoleAssignmentAndRoleMatchingResult(String roleName,
                                                                     String grantType) {

        RoleAssignment roleAssignment = RoleAssignment.builder()
            .roleName(roleName)
            .actorId(ACTOR_ID_1)
            .grantType(grantType)
            .authorisations(Lists.newArrayList(AUTHORISATION_1, AUTHORISATION_2))
            .readOnly(false)
            .build();
        return roleAssignment;
    }

    @Test
    void testGenerateAccessMetadataReturnsAccessProcessValueOfNone() {
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, GrantType.STANDARD.name());
        roleAndGrantType.put(ROLE_NAME_2, BASIC.name());

        CaseAccessMetadata caseAccessMetadata = getCaseAccessMetadata(roleAndGrantType, false);

        assertEquals(AccessProcess.NONE.name(), caseAccessMetadata.getAccessProcessString());
        assertEquals(String.join(",", BASIC.name(), GrantType.STANDARD.name()),
            caseAccessMetadata.getAccessGrantsString());
    }

    @Test
    void testGenerateAccessMetadataReturnsAccessProcessValueOfSpecific() {
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_2, BASIC.name());

        CaseAccessMetadata caseAccessMetadata = getCaseAccessMetadata(roleAndGrantType, false);

        assertEquals(AccessProcess.SPECIFIC.name(), caseAccessMetadata.getAccessProcessString());
        assertEquals(BASIC.name(), caseAccessMetadata.getAccessGrantsString());
    }

    @Test
    void testGenerateAccessMetadataWithPseudoRoleAssignmentsGeneration() {
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_2, BASIC.name());

        RoleAssignment roleAssignment1 = RoleAssignment.builder().grantType(CHALLENGED.name()).build();
        RoleAssignment roleAssignment2 = RoleAssignment.builder().grantType(GrantType.STANDARD.name()).build();

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        roleAssignments.add(roleAssignment1);
        roleAssignments.add(roleAssignment2);

        doReturn(roleAssignments)
            .when(pseudoRoleAssignmentsGenerator).createPseudoRoleAssignments(anyList());

        CaseAccessMetadata caseAccessMetadata = getCaseAccessMetadata(roleAndGrantType, true);

        assertEquals(AccessProcess.NONE.name(), caseAccessMetadata.getAccessProcessString());
        assertEquals(String.join(",", BASIC.name(), CHALLENGED.name(), STANDARD.name()),
            caseAccessMetadata.getAccessGrantsString());
    }

    @Test
    void testAnyRoleEqualsToWhenPassedUserRoleNotExistsInRoleAssignments() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, BASIC.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);
        doReturn(roleAssignments1).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        doReturn(false).when(applicationParams).getEnablePseudoAccessProfilesGeneration();

        boolean anyRoleEquals = defaultCaseDataAccessControl.anyAccessProfileEqualsTo(CASE_TYPE_1, "test");
        assertEquals(false, anyRoleEquals);
    }

    @Test
    void testAnyRoleEqualsToWhenPassedUserRoleExistsInRoleAssignments() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, BASIC.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);
        doReturn(roleAssignments1).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        doReturn(false).when(applicationParams).getEnablePseudoAccessProfilesGeneration();

        boolean anyRoleEquals = defaultCaseDataAccessControl.anyAccessProfileEqualsTo(CASE_TYPE_1, ROLE_NAME_1);
        assertEquals(true, anyRoleEquals);
    }

    @Test
    void testAnyRoleEqualsToWhenPassedUserRoleExistsInRoleAssignmentsWithPseudoGeneration() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1,
            ROLE_NAME_2,
            AccessControl.IDAM_PREFIX + ROLE_NAME_2);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, BASIC.name());
        roleAndGrantType.put(ROLE_NAME_2, BASIC.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);
        doReturn(roleAssignments1).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(true).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        RoleAssignment roleAssignment1 = RoleAssignment.builder()
            .roleName(AccessControl.IDAM_PREFIX + ROLE_NAME_2)
            .authorisations(Lists.newArrayList(AUTHORISATION_1))
            .grantType(BASIC.name()).build();

        List<RoleAssignment> pseudoRoleAssignments = new ArrayList<>();
        pseudoRoleAssignments.add(roleAssignment1);

        doReturn(pseudoRoleAssignments)
            .when(pseudoRoleAssignmentsGenerator).createPseudoRoleAssignments(anyList());

        boolean anyRoleEquals = defaultCaseDataAccessControl.anyAccessProfileEqualsTo(CASE_TYPE_1, ROLE_NAME_1);
        assertEquals(true, anyRoleEquals);

        anyRoleEquals = defaultCaseDataAccessControl.anyAccessProfileEqualsTo(CASE_TYPE_1,
            AccessControl.IDAM_PREFIX + ROLE_NAME_2);
        assertEquals(true, anyRoleEquals);
    }

    private CaseAccessMetadata getCaseAccessMetadata(Map<String, String> roleAndGrantType,
                                                     boolean enablePseudoRolesAssignmentGeneration) {
        doReturn(USER_ID).when(securityUtils).getUserId();
        CaseDetails caseDetails = new CaseDetails();
        Optional<CaseDetails> optionalCaseDetails = Optional.of(caseDetails);
        doReturn(optionalCaseDetails).when(caseDetailsRepository).findByReference(anyString());

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(enablePseudoRolesAssignmentGeneration)
            .when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        List<RoleAssignment> roleAssignmentsReturned = createFilteringResults(roleAndGrantType);
        doReturn(roleAssignmentsReturned)
            .when(roleAssignmentsFilteringService).filter(any(RoleAssignments.class), any(CaseDetails.class));

        return defaultCaseDataAccessControl.generateAccessMetadata("CASE_ID");
    }
}
