package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProcess;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleMatchingResult;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    private static final String GRANT_TYPE_BASIC = "BASIC";
    private static final String GRANT_TYPE_EXCLUDE = "EXCLUDED";
    private static final String GRANT_TYPE_STANDARD = "STANDARD";
    private static final String GRANT_TYPE_SPECIFIC = "SPECIFIC";
    private static final String GRANT_TYPE_CHALLENGED = "CHALLENGED";

    private static final String CASE_TYPE_1 = "TEST_CASE_TYPE";

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RoleAssignmentsFilteringService roleAssignmentsFilteringService;

    @Spy
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

    @InjectMocks
    private DefaultCaseDataAccessControl defaultCaseDataAccessControl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void generateAccessProfilesByCaseTypeId() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, GRANT_TYPE_BASIC);
        RoleAssignmentFilteringResult filteringResults = createFilteringResults(roleAndGrantType);
        doReturn(filteringResults).when(roleAssignmentsFilteringService)
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
        verify(accessProfileService).generateAccessProfiles(any(RoleAssignmentFilteringResult.class),
            anyList());
    }

    @Test
    void shouldReturnEmptyAccessProfilesWhenAccessProfilesHasExcludeGrantType() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, GRANT_TYPE_EXCLUDE);
        RoleAssignmentFilteringResult filteringResults = createFilteringResults(roleAndGrantType);
        doReturn(filteringResults).when(roleAssignmentsFilteringService)
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
        verify(accessProfileService).generateAccessProfiles(any(RoleAssignmentFilteringResult.class), anyList());
    }


    @Test
    void shouldReturnAccessProfilesWhenAccessProfilesHasExcludeGrantTypeAndOtherGrantTypes() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1, ROLE_NAME_4);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, GRANT_TYPE_BASIC);
        roleAndGrantType.put(ROLE_NAME_2, GRANT_TYPE_EXCLUDE);
        roleAndGrantType.put(ROLE_NAME_3, GRANT_TYPE_CHALLENGED);
        roleAndGrantType.put(ROLE_NAME_4, GRANT_TYPE_SPECIFIC);
        roleAndGrantType.put(ROLE_NAME_5, GRANT_TYPE_STANDARD);
        RoleAssignmentFilteringResult filteringResults = createFilteringResults(roleAndGrantType);

        doReturn(filteringResults).when(roleAssignmentsFilteringService)
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
        verify(accessProfileService).generateAccessProfiles(any(RoleAssignmentFilteringResult.class), anyList());
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
            roleToAccessProfileDefinitions.add(RoleToAccessProfileDefinition
                .builder()
                .roleName(roleName)
                .accessProfiles(roleName)
                .caseTypeId(CASE_TYPE_1)
                .disabled(false)
                .readOnly(false)
                .authorisations(AUTHORISATION_1)
                .build());
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

    private RoleAssignmentFilteringResult createFilteringResults(Map<String, String> roleNameAndGrantType) {
        List<Pair<RoleAssignment, RoleMatchingResult>> roleAssignmentMatchPairs = new ArrayList<>();

        roleNameAndGrantType.entrySet()
            .stream()
            .forEach(entry -> roleAssignmentMatchPairs
                .add(createRoleAssignmentAndRoleMatchingResult(entry.getKey(), entry.getValue())));

        RoleAssignmentFilteringResult result = new RoleAssignmentFilteringResult(roleAssignmentMatchPairs);
        return result;
    }

    private Pair<RoleAssignment, RoleMatchingResult> createRoleAssignmentAndRoleMatchingResult(String roleName,
                                                                                               String grantType) {
        RoleMatchingResult matchingResult = new RoleMatchingResult();
        matchingResult.setCaseTypeIdMatched(true);
        matchingResult.setClassificationMatched(true);
        matchingResult.setDateMatched(true);
        matchingResult.setLocationMatched(true);
        matchingResult.setRegionMatched(true);
        matchingResult.setCaseTypeIdMatched(true);
        matchingResult.setJurisdictionMatched(true);

        RoleAssignment roleAssignment = RoleAssignment.builder()
            .roleName(roleName)
            .actorId(ACTOR_ID_1)
            .grantType(grantType)
            .authorisations(Lists.newArrayList(AUTHORISATION_1, AUTHORISATION_2))
            .readOnly(false)
            .build();
        return Pair.of(roleAssignment, matchingResult);
    }

    @Test
    void testGenerateAccessMetadataReturnsAccessProcessValueOfNone() {
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, GRANT_TYPE_STANDARD);
        roleAndGrantType.put(ROLE_NAME_2, GRANT_TYPE_BASIC);

        CaseAccessMetadata caseAccessMetadata = getCaseAccessMetadata(roleAndGrantType, false);

        assertEquals(AccessProcess.NONE.name(), caseAccessMetadata.getAccessProcessString());
        assertEquals(String.join(",", GRANT_TYPE_BASIC, GRANT_TYPE_STANDARD),
            caseAccessMetadata.getAccessGrantsString());
    }

    @Test
    void testGenerateAccessMetadataReturnsAccessProcessValueOfSpecific() {
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_2, GRANT_TYPE_BASIC);

        CaseAccessMetadata caseAccessMetadata = getCaseAccessMetadata(roleAndGrantType, false);

        assertEquals(AccessProcess.SPECIFIC.name(), caseAccessMetadata.getAccessProcessString());
        assertEquals(GRANT_TYPE_BASIC, caseAccessMetadata.getAccessGrantsString());
    }

    @Test
    void testGenerateAccessMetadataWithPseudoRoleAssignmentsGeneration() {
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_2, GRANT_TYPE_BASIC);

        RoleAssignment roleAssignment1 = RoleAssignment.builder().grantType(GRANT_TYPE_CHALLENGED).build();
        RoleAssignment roleAssignment2 = RoleAssignment.builder().grantType(GRANT_TYPE_STANDARD).build();

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        roleAssignments.add(roleAssignment1);
        roleAssignments.add(roleAssignment2);

        doReturn(roleAssignments)
            .when(pseudoRoleAssignmentsGenerator).createPseudoRoleAssignments(any(RoleAssignmentFilteringResult.class));

        CaseAccessMetadata caseAccessMetadata = getCaseAccessMetadata(roleAndGrantType, true);

        assertEquals(AccessProcess.NONE.name(), caseAccessMetadata.getAccessProcessString());
        assertEquals(String.join(",", GRANT_TYPE_BASIC, GRANT_TYPE_CHALLENGED, GRANT_TYPE_STANDARD),
            caseAccessMetadata.getAccessGrantsString());
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

        RoleAssignmentFilteringResult filteringResults = createFilteringResults(roleAndGrantType);
        doReturn(filteringResults)
            .when(roleAssignmentsFilteringService).filter(any(RoleAssignments.class), any(CaseDetails.class));

        return defaultCaseDataAccessControl.generateAccessMetadata("CASE_ID");
    }
}
