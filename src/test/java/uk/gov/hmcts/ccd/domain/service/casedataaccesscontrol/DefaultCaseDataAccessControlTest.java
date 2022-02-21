package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
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
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.AuthorisationMapper;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment.builder;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.BASIC;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.CHALLENGED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.SPECIFIC;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.STANDARD;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;

@ExtendWith(MockitoExtension.class)
class DefaultCaseDataAccessControlTest {

    private static final String ROLE_NAME_1 = "TEST_ROLE_1";
    private static final String ROLE_NAME_2 = "TEST_ROLE_2";
    private static final String ROLE_NAME_3 = "TEST_ROLE_3";
    private static final String ROLE_NAME_4 = "TEST_ROLE_4";
    private static final String ROLE_NAME_5 = "TEST_ROLE_5";
    private static final String ACTOR_ID_1 = "ACTOR_ID_1";
    private static final String USER_ID = "USER_ID";
    private static final String CASE_ID = "45677";

    private static final String AUTHORISATION_1 = "TEST_AUTH_1";
    private static final String AUTHORISATION_2 = "TEST_AUTH_2";

    private static final String CASE_TYPE_1 = "TEST_CASE_TYPE";
    private HashMap<String, Predicate<AccessControlList>> accessMap;
    private final Set<String> userRoles = newHashSet(ROLE_NAME_1, ROLE_NAME_2, ROLE_NAME_3);

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RoleAssignmentsFilteringService roleAssignmentsFilteringService;

    @Mock
    private CaseTypeService caseTypeService;

    private final AccessProfileService accessProfileService =
        spy(new AccessProfileServiceImpl(new AuthorisationMapper(caseTypeService)));

    @Mock
    private PseudoRoleAssignmentsGenerator pseudoRoleAssignmentsGenerator;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock(lenient = true)
    private CaseDetailsRepository caseDetailsRepository;

    @Mock(lenient = true)
    private FilteredRoleAssignments filteredRoleAssignments;

    @Mock
    private UserAuthorisation userAuthorisation;

    @Mock
    private CaseUserRepository caseUserRepository;

    @InjectMocks
    private DefaultCaseDataAccessControl defaultCaseDataAccessControl;

    @BeforeEach
    void setUp() {
        this.accessMap = Maps.newHashMap();
        accessMap.put("create", CAN_CREATE);
        accessMap.put("update", CAN_UPDATE);
        accessMap.put("read", CAN_READ);
    }

    @Test
    void itShouldRemoveCaseDefinitionBaseOnRoleType() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignmentsForCreate(anyString());
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, BASIC.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);
        doReturn(roleAssignments1).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();
        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        var accessProfiles = createAccessProfiles(userRoles);
        var result = defaultCaseDataAccessControl
            .shouldRemoveCaseDefinition(accessProfiles, accessMap.get("create"), CASE_TYPE_1);

        verifyRemoveDefiniton(caseDefinitionRepository, securityUtils, roleAssignmentService,
            roleAssignmentsFilteringService, applicationParams, accessProfileService);
        assertFalse(result);
    }

    @Test
    void itShouldNotRemoveCaseDefinitionBaseOnRoleTypeDueReadAccess() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignmentsForCreate(anyString());
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, BASIC.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);
        doReturn(roleAssignments1).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();
        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        var accessProfiles = createAccessProfiles(userRoles);
        var result = defaultCaseDataAccessControl
            .shouldRemoveCaseDefinition(accessProfiles, accessMap.get("read"), CASE_TYPE_1);

        verifyRemoveDefiniton(caseDefinitionRepository, securityUtils, roleAssignmentService,
            roleAssignmentsFilteringService, applicationParams, accessProfileService);
        assertFalse(result);
    }

    @Test
    void shouldGenerateAccessProfilesByCaseDetails() {
        doReturn(USER_ID).when(securityUtils).getUserId();

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);

        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, GrantType.EXCLUDED.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);

        doReturn(roleAssignments1).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseDetails(caseDetails);

        assertNotNull(accessProfiles);
        assertEquals(0, accessProfiles.size());
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldGenerateAccessProfilesByCaseReference() {

        doReturn(USER_ID).when(securityUtils).getUserId();

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);
        Optional<CaseDetails> optionalCaseDetails = Optional.of(caseDetails);
        doReturn(optionalCaseDetails).when(caseDetailsRepository).findByReference(CASE_ID);

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);

        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, GrantType.EXCLUDED.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);

        doReturn(roleAssignments1).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl.generateAccessProfilesByCaseReference(CASE_ID);

        assertNotNull(accessProfiles);
        assertEquals(0, accessProfiles.size());
        verify(caseDetailsRepository).findByReference(CASE_ID);
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldGenerateAccessProfilesByNonExistingCaseId() {

        Optional<CaseDetails> optionalCaseDetails = Optional.empty();
        doReturn(optionalCaseDetails).when(caseDetailsRepository).findByReference(CASE_ID);

        Optional<CaseDetails> optionalCaseDetails1 = Optional.empty();
        doReturn(optionalCaseDetails1).when(caseDetailsRepository).findById(null, Long.parseLong(CASE_ID));

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl.generateAccessProfilesByCaseReference(CASE_ID);
        Assertions.assertTrue(accessProfiles.isEmpty());
        verify(caseDetailsRepository).findByReference(CASE_ID);
        verify(caseDetailsRepository).findById(null, Long.parseLong(CASE_ID));
    }

    @Test
    void itShouldNotRemoveCaseDefinitionBaseOnRoleTypeDueToEmptyRA() {

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignmentsForCreate(anyString());
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, BASIC.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);
        var filteredRoleAssignmentsEmpty = new ArrayList<>();
        doReturn(filteredRoleAssignmentsEmpty).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();
        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        var accessProfiles = createAccessProfiles(userRoles);
        var result = defaultCaseDataAccessControl
            .shouldRemoveCaseDefinition(accessProfiles, accessMap.get("create"), CASE_TYPE_1);

        verifyRemoveDefiniton(caseDefinitionRepository, securityUtils, roleAssignmentService,
            roleAssignmentsFilteringService, applicationParams, accessProfileService);
        assertTrue(result);
    }

    private static void verifyRemoveDefiniton(CaseDefinitionRepository caseDefinitionRepository,
                                              SecurityUtils securityUtils, RoleAssignmentService roleAssignmentService,
                                              RoleAssignmentsFilteringService roleAssignmentsFilteringService,
                                              ApplicationParams applicationParams,
                                              AccessProfileService accessProfileService) {

        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignmentsForCreate(anyString());
        verify(roleAssignmentsFilteringService).filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }


    private Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
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

        doReturn(roleAssignments1).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();
        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

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


        doReturn(roleAssignments1).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();
        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

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
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldReturnAccessProfilesWhenDefinitionHasDuplicateWithPseudoGeneratedRoleName() {
        CaseTypeDefinition caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1, CREATOR.name());

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        doReturn(USER_ID).when(securityUtils).getUserId();
        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, BASIC.name());
        roleAndGrantType.put(CREATOR.name(), BASIC.name());
        roleAndGrantType.put(ROLE_NAME_2, BASIC.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);

        doReturn(roleAssignments1).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();
        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        List<RoleToAccessProfileDefinition> generatedPseudoAP = List.of(
            RoleToAccessProfileDefinition.builder()
                .accessProfiles(CREATOR.name())
                .roleName(CREATOR.name())
                .build()
        );

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseTypeId(CASE_TYPE_1);

        assertNotNull(accessProfiles);
        assertEquals(2, accessProfiles.size());
    }

    private CaseTypeDefinition createCaseTypeDefinition(String... roleNames) {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(CASE_TYPE_1);
        caseTypeDefinition.setAccessControlLists(createAccessControlList());
        caseTypeDefinition.setRoleToAccessProfiles(createRoleToAccessProfileDefinitions(roleNames));
        Version version = new Version();
        version.setNumber(1);
        caseTypeDefinition.setVersion(version);
        return caseTypeDefinition;
    }

    private List<RoleToAccessProfileDefinition> createRoleToAccessProfileDefinitions(String... roleNames) {
        List<RoleToAccessProfileDefinition> roleToAccessProfileDefinitions = Lists.newArrayList();

        Arrays.stream(roleNames).forEach(roleName -> {
            roleToAccessProfileDefinitions.add(
                new RoleToAccessProfileDefinition(
                    CASE_TYPE_1, false, false, AUTHORISATION_1,
                    roleName, null, null, roleName, null
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
    void shouldGrantAccessToAccessLevelGrantedCreator() {
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.GRANTED);
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(CASE_ID);

        defaultCaseDataAccessControl.grantAccess(caseDetails, USER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(roleAssignmentService).createCaseRoleAssignments(
            eq(caseDetails), eq(USER_ID), captor.capture(), eq(false));
        verifyNoInteractions(caseUserRepository);
        assertEquals(1, captor.getValue().size());
        assertEquals(CREATOR.getRole(), captor.getValue().iterator().next());
    }

    @Test
    void shouldGrantAccessToAccessLevelGrantedCreatorAndSyncCaseUsers() {
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.GRANTED);
        when(applicationParams.getEnableCaseUsersDbSync()).thenReturn(true);
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(CASE_ID);

        defaultCaseDataAccessControl.grantAccess(caseDetails, USER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(roleAssignmentService).createCaseRoleAssignments(
            eq(caseDetails), eq(USER_ID), captor.capture(), eq(false));
        verify(caseUserRepository).grantAccess(Long.valueOf(CASE_ID), USER_ID, CREATOR.getRole());
        assertEquals(1, captor.getValue().size());
        assertEquals(CREATOR.getRole(), captor.getValue().iterator().next());
    }

    @Test
    void shouldNotGrantAccessToAccessLevelAllCreator() {
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.ALL);
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(CASE_ID);

        defaultCaseDataAccessControl.grantAccess(caseDetails, USER_ID);

        verifyNoInteractions(roleAssignmentService);
        verifyNoInteractions(caseUserRepository);
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

        RoleAssignment roleAssignment = builder()
            .roleName(roleName)
            .actorId(ACTOR_ID_1)
            .grantType(grantType)
            .authorisations(Lists.newArrayList(AUTHORISATION_1, AUTHORISATION_2))
            .readOnly(false)
            .build();
        return roleAssignment;
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
        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));
        when(filteredRoleAssignments.getFilteredMatchingRoleAssignments()).thenReturn(roleAssignments1);

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

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
        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));
        when(filteredRoleAssignments.getFilteredMatchingRoleAssignments()).thenReturn(roleAssignments1);

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

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
        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseTypeDefinition.class));
        when(filteredRoleAssignments.getFilteredMatchingRoleAssignments()).thenReturn(roleAssignments1);

        doReturn(true).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        RoleAssignment roleAssignment1 = RoleAssignment.builder()
            .roleName(AccessControl.IDAM_PREFIX + ROLE_NAME_2)
            .authorisations(Lists.newArrayList(AUTHORISATION_1))
            .grantType(BASIC.name()).build();

        List<RoleAssignment> pseudoRoleAssignments = new ArrayList<>();
        pseudoRoleAssignments.add(roleAssignment1);

        doReturn(pseudoRoleAssignments)
            .when(pseudoRoleAssignmentsGenerator).createPseudoRoleAssignments(anyList(), eq(false));

        boolean anyRoleEquals = defaultCaseDataAccessControl.anyAccessProfileEqualsTo(CASE_TYPE_1, ROLE_NAME_1);
        assertEquals(true, anyRoleEquals);

        anyRoleEquals = defaultCaseDataAccessControl.anyAccessProfileEqualsTo(CASE_TYPE_1,
            AccessControl.IDAM_PREFIX + ROLE_NAME_2);
        assertEquals(true, anyRoleEquals);
    }

    @Test
    void testGenerateAccessMetadataWithNoCaseId() {
        CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadataWithNoCaseId();

        assertEquals(AccessProcess.NONE.name(), caseAccessMetadata.getAccessProcessString());
        assertEquals(STANDARD.name(), caseAccessMetadata.getAccessGrantsString());
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

        RoleAssignment roleAssignment1 = builder().grantType(CHALLENGED.name()).build();
        RoleAssignment roleAssignment2 = builder().grantType(GrantType.STANDARD.name()).build();

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        roleAssignments.add(roleAssignment1);
        roleAssignments.add(roleAssignment2);

        doReturn(roleAssignments)
            .when(pseudoRoleAssignmentsGenerator).createPseudoRoleAssignments(anyList(), eq(false));

        CaseAccessMetadata caseAccessMetadata = getCaseAccessMetadata(roleAndGrantType, true);

        assertEquals(AccessProcess.NONE.name(), caseAccessMetadata.getAccessProcessString());
        assertEquals(String.join(",", BASIC.name(), CHALLENGED.name(), STANDARD.name()),
            caseAccessMetadata.getAccessGrantsString());
    }

    @Test
    void testGenerateAccessMetadataReturnsAccessProcessValueOfChallenged() {
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_2, BASIC.name());

        List<RoleAssignment> roleAssignmentsReturned = Collections.singletonList(RoleAssignment.builder().build());
        doReturn(roleAssignmentsReturned)
            .when(filteredRoleAssignments)
            .getFilteredRoleAssignmentsFailedOnRegionOrBaseLocationMatcher();

        CaseAccessMetadata caseAccessMetadata = getCaseAccessMetadata(roleAndGrantType, false);

        assertEquals(AccessProcess.CHALLENGED.name(), caseAccessMetadata.getAccessProcessString());
        assertEquals(BASIC.name(), caseAccessMetadata.getAccessGrantsString());
    }

    @Test
    void testGenerateAccessMetadataReturnsAccessProcessValueOfSpecificWhenNoRegionOrLocationRoleAssignmentsExist() {
        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_2, STANDARD.name());

        doReturn(Collections.emptyList())
            .when(filteredRoleAssignments)
            .getFilteredRoleAssignmentsFailedOnRegionOrBaseLocationMatcher();

        CaseAccessMetadata caseAccessMetadata = getCaseAccessMetadata(roleAndGrantType, false);

        assertEquals(AccessProcess.NONE.name(), caseAccessMetadata.getAccessProcessString());
        assertEquals(STANDARD.name(), caseAccessMetadata.getAccessGrantsString());
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
        doReturn(roleAssignmentsReturned).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();


        doReturn(filteredRoleAssignments)
                .when(roleAssignmentsFilteringService).filter(
                    any(RoleAssignments.class), any(CaseDetails.class));
        return defaultCaseDataAccessControl.generateAccessMetadata("CASE_ID");
    }
}
