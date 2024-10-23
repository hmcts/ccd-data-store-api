package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProcess;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.MatcherType;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.AuthorisationMapper;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.endpoint.exceptions.DownstreamIssueException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class AccessProfileServiceCalls {
        private List<List<RoleAssignment>> accessGrantedChecks;
        private List<List<RoleAssignment>> accessProcessChecks;

        public List<RoleAssignment> accessProcessRoleAssignmentsCheckedInFirstCall() {
            // NB: first AP check contains all relevant RAs.
            return this.accessProcessChecks.get(0);
        }

        public List<RoleAssignment> allAccessGrantedRoleAssignmentsChecked() {
            // NB: AG checks are against individual RAs: so flatten list of lists for easier processing.
            return this.accessGrantedChecks.stream()
                .flatMap(Collection::stream).collect(Collectors.toList());
        }
    }

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

    private static final String CASE_STATE_1 = "TEST_CASE_STATE";
    private static final String CASE_TYPE_1 = "TEST_CASE_TYPE";
    private HashMap<String, Predicate<AccessControlList>> accessMap;
    private final Set<String> userRoles = newHashSet(ROLE_NAME_1, ROLE_NAME_2, ROLE_NAME_3);

    private List<AccessProfile> dummyAccessProfiles;

    private final ObjectMapper mapper = JacksonUtils.MAPPER;

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RoleAssignmentsFilteringService roleAssignmentsFilteringService;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock(lenient = true)
    AccessControlService accessControlService;

    private final AccessProfileService accessProfileService =
        spy(new AccessProfileServiceImpl(new AuthorisationMapper(caseTypeService)));

    @Mock
    private PseudoRoleAssignmentsGenerator pseudoRoleAssignmentsGenerator;

    @Mock
    private PseudoRoleToAccessProfileGenerator pseudoRoleToAccessProfileGenerator;

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

        dummyAccessProfiles = List.of(
            AccessProfile.builder()
                .accessProfile("ap_1")
                .caseAccessCategories(null)
                .build(),
            AccessProfile.builder()
                .accessProfile("ap_2")
                .caseAccessCategories("UNSPEC_CLAIM")
                .build(),
            AccessProfile.builder()
                .accessProfile("ap_3")
                .caseAccessCategories("SPEC_CLAIM")
                .build(),
            AccessProfile.builder()
                .accessProfile("ap_4")
                .caseAccessCategories("SOME_OTHER_CLAIM,UNSPEC_CLAIM,AND_ANOTHER")
                .build(),
            AccessProfile.builder()
                .accessProfile("ap_5")
                .caseAccessCategories("SOME_CLAIM, TRIM_CLAIM")
                .build(),
            AccessProfile.builder()
                .accessProfile("ap_6")
                .caseAccessCategories("DUMMY_CLAIM_1, DUMMY_CLAIM_2")
                .build());
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

        verifyRemoveDefinition(caseDefinitionRepository, securityUtils, roleAssignmentService,
            roleAssignmentsFilteringService, applicationParams, accessProfileService);
        assertFalse(result);
    }

    @Test
    void itShouldNotRemoveCaseDefinitionBaseOnRoleTypeDueReadAccess() {
        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);

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

        verifyRemoveDefinition(caseDefinitionRepository, securityUtils, roleAssignmentService,
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
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);

        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, GrantType.EXCLUDED.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);

        doReturn(roleAssignments1).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseDetails(caseDetails);

        assertNotNull(accessProfiles);
        assertEquals(0, accessProfiles.size());
        verify(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldGenerateAccessProfilesByCaseAccessCategory_MatchCommaSeparatedLine() throws JsonProcessingException {
        doReturn(USER_ID).when(securityUtils).getUserId();
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);
        caseDetails.setData(generateCaseData("UNSPEC_CLAIM"));

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);

        doReturn(Collections.emptyList()).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        doReturn(dummyAccessProfiles).when(accessProfileService).generateAccessProfiles(anyList(), anyList());

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseDetails(caseDetails);

        assertNotNull(accessProfiles);
        assertEquals(3, accessProfiles.size());
        assertTrue(Stream.of("ap_1", "ap_2", "ap_4")
            .allMatch(s -> accessProfiles.stream()
                .anyMatch(ap -> s.equals(ap.getAccessProfile()))));

        verify(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldGenerateAccessProfilesByCaseAccessCategory_NoCaseAccessCategoryMatch() throws JsonProcessingException {
        doReturn(USER_ID).when(securityUtils).getUserId();
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);
        caseDetails.setData(generateCaseData("NO_MATCH"));

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);

        doReturn(Collections.emptyList()).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        doReturn(dummyAccessProfiles).when(accessProfileService).generateAccessProfiles(anyList(), anyList());

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseDetails(caseDetails);

        assertNotNull(accessProfiles);
        assertEquals(1, accessProfiles.size());
        assertTrue(accessProfiles.stream().allMatch(a -> a.getAccessProfile().equals("ap_1")));

        verify(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldGenerateAccessProfilesByCaseAccessCategory_MatchOnlySingleLineCaseAccessCategory()
        throws JsonProcessingException {
        doReturn(USER_ID).when(securityUtils).getUserId();
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);
        caseDetails.setData(generateCaseData("SPEC_CLAIM"));

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);

        doReturn(Collections.emptyList()).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        doReturn(dummyAccessProfiles).when(accessProfileService).generateAccessProfiles(anyList(), anyList());

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseDetails(caseDetails);

        assertNotNull(accessProfiles);
        assertEquals(2, accessProfiles.size());
        assertTrue(Stream.of("ap_1", "ap_3")
            .allMatch(s -> accessProfiles.stream()
                .anyMatch(ap -> s.equals(ap.getAccessProfile()))));

        verify(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldGenerateAccessProfilesByCaseAccessCategory_MatchTrimmedCaseAccessCategory()
        throws JsonProcessingException {
        doReturn(USER_ID).when(securityUtils).getUserId();
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);
        caseDetails.setData(generateCaseData("TRIM_CLAIM"));

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);

        doReturn(Collections.emptyList()).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        doReturn(dummyAccessProfiles).when(accessProfileService).generateAccessProfiles(anyList(), anyList());

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseDetails(caseDetails);

        assertNotNull(accessProfiles);
        assertEquals(2, accessProfiles.size());
        assertTrue(Stream.of("ap_1", "ap_5")
                .allMatch(s -> accessProfiles.stream()
                    .anyMatch(ap -> s.equals(ap.getAccessProfile()))));

        verify(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldGenerateAccessProfilesByCaseAccessCategory_EmptyCaseAccessCategoryField()
        throws JsonProcessingException {
        doReturn(USER_ID).when(securityUtils).getUserId();
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);
        caseDetails.setData(generateCaseData(""));

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);

        doReturn(Collections.emptyList()).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        doReturn(dummyAccessProfiles).when(accessProfileService).generateAccessProfiles(anyList(), anyList());

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseDetails(caseDetails);

        assertNotNull(accessProfiles);
        assertEquals(1, accessProfiles.size());
        assertTrue(accessProfiles.stream().allMatch(a -> a.getAccessProfile().equals("ap_1")));

        verify(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldGenerateAccessProfilesByCaseAccessCategory_NullCaseAccessCategoryField()
        throws JsonProcessingException {
        doReturn(USER_ID).when(securityUtils).getUserId();
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);
        caseDetails.setData(generateCaseData(null));

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);

        doReturn(Collections.emptyList()).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        doReturn(dummyAccessProfiles).when(accessProfileService).generateAccessProfiles(anyList(), anyList());

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseDetails(caseDetails);

        assertNotNull(accessProfiles);
        assertEquals(1, accessProfiles.size());
        assertTrue(accessProfiles.stream().allMatch(a -> a.getAccessProfile().equals("ap_1")));

        verify(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldGenerateAccessProfilesByCaseAccessCategory_StartWithNoMatch()
        throws JsonProcessingException {
        doReturn(USER_ID).when(securityUtils).getUserId();
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);
        caseDetails.setData(generateCaseData("DUMMY_CLAIM"));

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);

        doReturn(Collections.emptyList()).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        doReturn(dummyAccessProfiles).when(accessProfileService).generateAccessProfiles(anyList(), anyList());

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseDetails(caseDetails);

        assertNotNull(accessProfiles);
        assertEquals(1, accessProfiles.size());
        assertTrue(accessProfiles.stream().allMatch(a -> a.getAccessProfile().equals("ap_1")));

        verify(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));
        verify(applicationParams).getEnablePseudoRoleAssignmentsGeneration();
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldGenerateAccessProfilesByCaseAccessCategory_StartWithMatch()
        throws JsonProcessingException {
        doReturn(USER_ID).when(securityUtils).getUserId();
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);
        caseDetails.setData(generateCaseData("DUMMY_CLAIM_2345"));

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);

        doReturn(Collections.emptyList()).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        doReturn(dummyAccessProfiles).when(accessProfileService).generateAccessProfiles(anyList(), anyList());

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseDetails(caseDetails);

        assertNotNull(accessProfiles);
        assertEquals(2, accessProfiles.size());
        assertTrue(Stream.of("ap_1", "ap_6")
            .allMatch(s -> accessProfiles.stream()
                .anyMatch(ap -> s.equals(ap.getAccessProfile()))));

        verify(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);
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
        assertTrue(accessProfiles.isEmpty());
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

        verifyRemoveDefinition(caseDefinitionRepository, securityUtils, roleAssignmentService,
            roleAssignmentsFilteringService, applicationParams, accessProfileService);
        assertTrue(result);
    }

    private static void verifyRemoveDefinition(CaseDefinitionRepository caseDefinitionRepository,
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

        roleNameAndGrantType.forEach((key, value) -> roleAssignments
            .add(createRoleAssignmentAndRoleMatchingResult(key, value)));

        return roleAssignments;
    }

    private RoleAssignment createRoleAssignmentAndRoleMatchingResult(String roleName,
                                                                     String grantType) {

        return builder()
            .roleName(roleName)
            .actorId(ACTOR_ID_1)
            .grantType(grantType)
            .authorisations(Lists.newArrayList(AUTHORISATION_1, AUTHORISATION_2))
            .readOnly(false)
            .build();
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
        assertFalse(anyRoleEquals);
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
        assertTrue(anyRoleEquals);
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
        assertTrue(anyRoleEquals);

        anyRoleEquals = defaultCaseDataAccessControl.anyAccessProfileEqualsTo(CASE_TYPE_1,
            AccessControl.IDAM_PREFIX + ROLE_NAME_2);
        assertTrue(anyRoleEquals);
    }

    @Test
    void testGenerateAccessMetadataWithNoCaseId() {
        CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadataWithNoCaseId();

        assertEquals(AccessProcess.NONE.name(), caseAccessMetadata.getAccessProcessString());
        assertEquals(STANDARD.name(), caseAccessMetadata.getAccessGrantsString());
    }

    @Test
    void testGetUserClassificationsNullSecurityClassificationAndExpectValidationException() {
        doReturn(USER_ID).when(securityUtils).getUserId();

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);

        RoleAssignments roleAssignments = new RoleAssignments();
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class));

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getScopedCachedCaseType(CASE_TYPE_1);

        Map<String, String> roleAndGrantType = Maps.newHashMap();
        roleAndGrantType.put(ROLE_NAME_1, STANDARD.name());
        List<RoleAssignment> roleAssignments1 = createFilteringResults(roleAndGrantType);

        doReturn(roleAssignments1).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        doReturn(false).when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

        assertThatExceptionOfType(DownstreamIssueException.class)
            .isThrownBy(() -> defaultCaseDataAccessControl.getUserClassifications(caseDetails))
            .withMessage("null SecurityClassification for role: TEST_ROLE_1");
    }

    @Test
    void shouldGenerateAccessProfilesForRestrictedCases() {
        doReturn(USER_ID).when(securityUtils).getUserId();

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);

        RoleAssignment roleAssignment1 = RoleAssignment.builder()
            .grantType(BASIC.name())
            .roleName(ROLE_NAME_3)
            .classification(Classification.PUBLIC.name())
            .build();
        RoleAssignment roleAssignment2 = RoleAssignment.builder()
            .grantType(STANDARD.name())
            .roleName(ROLE_NAME_5)
            .classification(Classification.PUBLIC.name())
            .build();
        dummyAccessProfiles = List.of(
            AccessProfile.builder()
                .accessProfile(ROLE_NAME_3)
                .caseAccessCategories(null)
                .build());

        RoleAssignments roleAssignments = new RoleAssignments();
        roleAssignments.setRoleAssignments(List.of(roleAssignment1, roleAssignment2));
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class), anyList());

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_3);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);

        doReturn(List.of(roleAssignment1)).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();
        doReturn(dummyAccessProfiles).when(accessProfileService)
            .generateAccessProfiles(argThat(list -> list.size() == 1 && list.get(0) == roleAssignment1), any());

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesForRestrictedCase(caseDetails);
        Set<String> userRole = newHashSet(ROLE_NAME_3);
        assertNotNull(accessProfiles);
        assertEquals(1, accessProfiles.size());
        Assert.assertTrue(userRole.contains(accessProfiles.iterator().next().getAccessProfile()));
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_1);
        verify(securityUtils).getUserId();
        verify(roleAssignmentService).getRoleAssignments(anyString());
        verify(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class),
                argThat(list -> list.contains(MatcherType.SECURITYCLASSIFICATION)));
        verify(accessProfileService).generateAccessProfiles(anyList(), anyList());
    }

    @Test
    void shouldNotGenerateAccessProfilesWithInvalidRoleAssignmentsForRestrictedCases() {
        doReturn(USER_ID).when(securityUtils).getUserId();

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_1);

        RoleAssignment roleAssignmentWithInvalidName = RoleAssignment.builder()
            .grantType(BASIC.name())
            .roleName(ROLE_NAME_1)
            .classification(Classification.PUBLIC.name())
            .build();
        RoleAssignment roleAssignmentWithInvalidGrantType = RoleAssignment.builder()
            .grantType(STANDARD.name())
            .roleName(ROLE_NAME_3)
            .classification(Classification.PUBLIC.name())
            .build();

        RoleAssignments roleAssignments = new RoleAssignments();
        roleAssignments.setRoleAssignments(List.of(roleAssignmentWithInvalidName, roleAssignmentWithInvalidGrantType));
        doReturn(roleAssignments).when(roleAssignmentService).getRoleAssignments(anyString());

        doReturn(filteredRoleAssignments).when(roleAssignmentsFilteringService)
            .filter(any(RoleAssignments.class), any(CaseDetails.class), anyList());

        CaseTypeDefinition caseTypeDefinition = createCaseTypeDefinition(ROLE_NAME_1, ROLE_NAME_3);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);

        doReturn(List.of()).when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

        Set<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesForRestrictedCase(caseDetails);

        assertNotNull(accessProfiles);
        assertEquals(0, accessProfiles.size());
        verify(roleAssignmentsFilteringService)
            .filter(argThat(ras -> ras.getRoleAssignments().isEmpty()), any(CaseDetails.class), any());
    }


    @Nested
    class GenerateAccessMetadata {

        private CaseDetails caseDetails;

        private CaseTypeDefinition caseTypeDefinition;

        private RoleAssignments usersRoleAssignments;

        private List<RoleAssignment> filteredMatchingRoleAssignments;

        private List<RoleAssignment> pseudoRoleAssignments;

        @Captor
        private ArgumentCaptor<List<RoleAssignment>> roleAssignmentListCaptor;

        @BeforeEach
        void setup() {
            caseDetails = new CaseDetails();
            caseDetails.setCaseTypeId(CASE_TYPE_1);
            caseDetails.setState(CASE_STATE_1);

            caseTypeDefinition =  createCaseTypeDefinition(ROLE_NAME_1);

            usersRoleAssignments = new RoleAssignments();

            pseudoRoleAssignments = new ArrayList<>();
        }

        @Test
        void generateAccessMetadata_returnsEmptyMetadataIfCaseNotFound() {

            // GIVEN
            doReturn(Optional.empty()).when(caseDetailsRepository).findByReference(CASE_ID);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertNotNull(caseAccessMetadata);
            assertNull(caseAccessMetadata.getAccessGrants());
            assertNull(caseAccessMetadata.getAccessProcess());

        }

        @Test
        void generateAccessMetadata_loadsRoleAssignmentsAndPassesToFilterService() {

            // GIVEN
            setupStandardMocks(new HashMap<>(), false);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertNotNull(caseAccessMetadata);
            verify(roleAssignmentsFilteringService).filter(usersRoleAssignments, caseDetails);

        }

        @Test
        void generateAccessMetadata_verifyAccessProfileServiceCall_skipCallsIfCaseTypeNotLoaded() {

            // GIVEN
            String caseType = "not-found";
            caseDetails.setCaseTypeId(caseType);

            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_1, STANDARD.name());
            roleAndGrantType.put(ROLE_NAME_2, STANDARD.name());
            setupStandardMocks(roleAndGrantType, false);

            // reset and replace standard CaseType mock
            Mockito.reset(caseDefinitionRepository);
            doReturn(null).when(caseDefinitionRepository).getCaseType(caseType);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertNotNull(caseAccessMetadata);
            verifyNoInteractions(accessProfileService);
            verifyNoInteractions(accessControlService);

            assertEquals(AccessProcess.SPECIFIC, caseAccessMetadata.getAccessProcess());

        }

        @Test
        void generateAccessMetadata_verifyAccessProfileServiceCall_onlyProcessesRAsThatAreRelevantForAGOrAPChecks() {

            // GIVEN
            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_1, BASIC.name());
            roleAndGrantType.put(ROLE_NAME_2, CHALLENGED.name());
            roleAndGrantType.put(ROLE_NAME_3, SPECIFIC.name());
            roleAndGrantType.put(ROLE_NAME_4, STANDARD.name());
            setupStandardMocks(roleAndGrantType, false);

            // NB: now filter RAs down to those the AccessProfile checks use
            List<RoleAssignment> expectedAccessProcessRoleAssignments
                = filterRoleAssignmentsForAccessProfileServiceCall(filteredMatchingRoleAssignments);

            setupAccessGrantedCheckMocks_allSuccess(filteredMatchingRoleAssignments);
            setupAccessProfileCheckMocks_firstSucceeds(expectedAccessProcessRoleAssignments);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertNotNull(caseAccessMetadata);
            verifyNoInteractions(pseudoRoleAssignmentsGenerator);

            AccessProfileServiceCalls apServiceCalls = verifyAccessProfileServiceCalls();

            // verify AccessGranted check only use filtered+matched RAs that are Basic, Standard, Specific or Challenged
            // NB: filter RAs down to those the AccessGranted checks use
            List<RoleAssignment> expectedAccessGrantedRoleAssignments
                = filterRoleAssignmentsForAccessGrantedServiceCall(filteredMatchingRoleAssignments);
            List<RoleAssignment> roleAssignmentAGList = apServiceCalls.allAccessGrantedRoleAssignmentsChecked();
            assertEquals(expectedAccessGrantedRoleAssignments.size(), roleAssignmentAGList.size());
            assertTrue(roleAssignmentAGList.containsAll(expectedAccessGrantedRoleAssignments));

            // verify AccessProcess check only use filtered+matched RAs that are Standard, Specific or Challenged
            assertEquals(1, apServiceCalls.accessProcessChecks.size());
            List<RoleAssignment> roleAssignmentAPList = apServiceCalls.accessProcessRoleAssignmentsCheckedInFirstCall();
            assertEquals(expectedAccessProcessRoleAssignments.size(), roleAssignmentAPList.size());
            assertTrue(roleAssignmentAPList.containsAll(expectedAccessProcessRoleAssignments));
        }

        @Test
        void generateAccessMetadata_verifyAccessProfileServiceCall_pseudoRoleAssignmentsGeneration_disabled() {

            // GIVEN
            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_1, STANDARD.name());
            roleAndGrantType.put(ROLE_NAME_2, STANDARD.name());
            setupStandardMocks(roleAndGrantType, false);

            setupAccessGrantedCheckMocks_allSuccess(filteredMatchingRoleAssignments);
            setupAccessProfileCheckMocks_firstSucceeds(filteredMatchingRoleAssignments);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertNotNull(caseAccessMetadata);
            verifyNoInteractions(pseudoRoleAssignmentsGenerator);

            AccessProfileServiceCalls apServiceCalls = verifyAccessProfileServiceCalls();

            // verify AccessGranted check used RAs contain only filtered+matched RAs (i.e. no pseudo RAs)
            List<RoleAssignment> roleAssignmentAGList = apServiceCalls.allAccessGrantedRoleAssignmentsChecked();
            assertEquals(filteredMatchingRoleAssignments.size(), roleAssignmentAGList.size());
            assertTrue(roleAssignmentAGList.containsAll(filteredMatchingRoleAssignments));

            // verify AccessProcess check used RAs contain only filtered+matched RAs (i.e. no pseudo RAs)
            List<RoleAssignment> roleAssignmentAPList = apServiceCalls.accessProcessRoleAssignmentsCheckedInFirstCall();
            assertEquals(filteredMatchingRoleAssignments.size(), roleAssignmentAPList.size());
            assertTrue(roleAssignmentAPList.containsAll(filteredMatchingRoleAssignments));
        }

        @Test
        void generateAccessMetadata_verifyAccessProfileServiceCall_pseudoRoleAssignmentsGeneration_enabled() {

            // GIVEN
            addPseudoRoleAssignments(List.of(ROLE_NAME_1, ROLE_NAME_2));

            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_3, STANDARD.name());
            roleAndGrantType.put(ROLE_NAME_4, STANDARD.name());
            setupStandardMocks(roleAndGrantType, true);

            List<RoleAssignment> filteredAndPseudoRolesAssignments = new ArrayList<>();
            filteredAndPseudoRolesAssignments.addAll(filteredMatchingRoleAssignments);
            filteredAndPseudoRolesAssignments.addAll(pseudoRoleAssignments);

            setupAccessGrantedCheckMocks_allSuccess(filteredAndPseudoRolesAssignments);
            setupAccessProfileCheckMocks_firstSucceeds(filteredMatchingRoleAssignments);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertNotNull(caseAccessMetadata);
            verify(pseudoRoleAssignmentsGenerator).createPseudoRoleAssignments(filteredMatchingRoleAssignments, false);

            AccessProfileServiceCalls apServiceCalls = verifyAccessProfileServiceCalls();

            // verify AccessGranted check used RAs contain only filtered+matched RAs & pseudo RAs
            List<RoleAssignment> roleAssignmentAGList = apServiceCalls.allAccessGrantedRoleAssignmentsChecked();
            assertEquals(filteredAndPseudoRolesAssignments.size(), roleAssignmentAGList.size());
            assertTrue(roleAssignmentAGList.containsAll(filteredAndPseudoRolesAssignments));

            // verify AccessProcess check used RAs contain only filtered+matched RAs & pseudo RAs
            List<RoleAssignment> roleAssignmentAPList = apServiceCalls.accessProcessRoleAssignmentsCheckedInFirstCall();
            assertEquals(filteredAndPseudoRolesAssignments.size(), roleAssignmentAPList.size());
            assertTrue(roleAssignmentAPList.containsAll(filteredAndPseudoRolesAssignments));
        }

        @Test
        void generateAccessMetadata_verifyAccessProfileServiceCall_roleToAccessProfilesMappings_none() {

            // GIVEN
            caseTypeDefinition.setRoleToAccessProfiles(List.of());

            List<RoleToAccessProfileDefinition> pseudoGeneratedR2AP =
                createRoleToAccessProfileDefinitions("PseudoRole1", "PseudoRole2");
            doReturn(pseudoGeneratedR2AP).when(pseudoRoleToAccessProfileGenerator).generate(caseTypeDefinition);

            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_1, STANDARD.name());
            roleAndGrantType.put(ROLE_NAME_2, STANDARD.name());
            setupStandardMocks(roleAndGrantType, false);

            setupAccessGrantedCheckMocks_allSuccess(filteredMatchingRoleAssignments);
            setupAccessProfileCheckMocks_firstSucceeds(filteredMatchingRoleAssignments);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertNotNull(caseAccessMetadata);
            verifyNoInteractions(pseudoRoleAssignmentsGenerator);

            // verify Pseudo RoleToAccessProfilesMappings passed to AccessProfileService
            int numberOfCalls = numberOfExpectedGenerateAccessProfileCalls(filteredMatchingRoleAssignments);
            verify(accessProfileService, times(numberOfCalls)).generateAccessProfiles(any(), eq(pseudoGeneratedR2AP));
        }

        @Test
        void generateAccessMetadata_verifyAccessProfileServiceCall_roleToAccessProfilesMappings_some() {

            // GIVEN
            List<RoleToAccessProfileDefinition> caseTypeR2AP
                = createRoleToAccessProfileDefinitions(ROLE_NAME_1, ROLE_NAME_2);
            caseTypeDefinition.setRoleToAccessProfiles(caseTypeR2AP);

            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_1, STANDARD.name());
            roleAndGrantType.put(ROLE_NAME_2, STANDARD.name());
            setupStandardMocks(roleAndGrantType, false);

            setupAccessGrantedCheckMocks_allSuccess(filteredMatchingRoleAssignments);
            setupAccessProfileCheckMocks_firstSucceeds(filteredMatchingRoleAssignments);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertNotNull(caseAccessMetadata);
            verifyNoInteractions(pseudoRoleToAccessProfileGenerator);

            // verify CaseType RoleToAccessProfilesMappings passed to AccessProfileService
            int numberOfCalls = numberOfExpectedGenerateAccessProfileCalls(filteredMatchingRoleAssignments);
            verify(accessProfileService, times(numberOfCalls)).generateAccessProfiles(any(), eq(caseTypeR2AP));
        }

        @ParameterizedTest(
            name = "generateAccessMetadata_verifyAccessProfileServiceCall_SecondCallIfAPChecksFail: {0}, {1}, {2}"
        )
        @MethodSource(ACCESS_PROFILE_CHECK_NEGATIVE_MATCH_PARAMS)
        void generateAccessMetadata_verifyAccessProfileServiceCall_secondCallIfAPChecksFail(
            List<String> outputAccessProfiles,
            boolean canAccessCaseType,
            boolean canAccessCaseState
        ) {

            // GIVEN
            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_1, STANDARD.name());
            roleAndGrantType.put(ROLE_NAME_2, STANDARD.name());
            setupStandardMocks(roleAndGrantType, false);

            setupAccessGrantedCheckMocks_allSuccess(filteredMatchingRoleAssignments);
            // make the first AP check FAIL
            setupAccessProfileCheckMocks(
                filteredMatchingRoleAssignments, outputAccessProfiles, canAccessCaseType, canAccessCaseState
            );

            // make the second AP check SUCCEED
            @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
            List<RoleAssignment> roleAssignmentsRegionOrLocation =
                setupSecondAccessProfileCheckMocks(List.of(ROLE_NAME_1), List.of("AP"), true, true);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertNotNull(caseAccessMetadata);
            verifyNoInteractions(pseudoRoleAssignmentsGenerator);

            AccessProfileServiceCalls apServiceCalls = verifyAccessProfileServiceCalls();

            // verify AccessProfileService Called twice for AccessProcess Checks
            // call 1
            List<RoleAssignment> roleAssignmentList1 = apServiceCalls.accessProcessRoleAssignmentsCheckedInFirstCall();
            assertEquals(filteredMatchingRoleAssignments.size(), roleAssignmentList1.size());
            assertTrue(roleAssignmentList1.containsAll(filteredMatchingRoleAssignments));
            // call 2
            List<RoleAssignment> roleAssignmentList2 = apServiceCalls.accessProcessChecks.get(1);
            assertEquals(roleAssignmentsRegionOrLocation.size(), roleAssignmentList2.size());
            assertTrue(roleAssignmentList2.containsAll(roleAssignmentsRegionOrLocation));
        }

        @ParameterizedTest(
            name = "generateAccessMetadata_returnsAccessGrantedList_excludingThoseWhenAGChecksFail: {0}, {1}, {2}"
        )
        @MethodSource(ACCESS_GRANTED_CHECK_NEGATIVE_MATCH_PARAMS)
        void generateAccessMetadata_returnsAccessGrantedList_excludingThoseWhenAGChecksFail(
            List<String> outputAccessProfiles,
            boolean canAccessCaseType,
            boolean canAccessCaseState
        ) {

            // GIVEN
            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_1, BASIC.name());
            roleAndGrantType.put(ROLE_NAME_2, CHALLENGED.name());
            roleAndGrantType.put(ROLE_NAME_3, SPECIFIC.name());
            roleAndGrantType.put(ROLE_NAME_4, STANDARD.name());
            setupStandardMocks(roleAndGrantType, false);

            // SUCCESS for role 1
            RoleAssignment roleAssignment1 = filteredMatchingRoleAssignments.get(0);
            setupAccessGrantedCheckMocks(roleAssignment1, List.of("AG1"), true, true);
            // FAIL for role 2
            RoleAssignment roleAssignment2 = filteredMatchingRoleAssignments.get(1);
            setupAccessGrantedCheckMocks(roleAssignment2, outputAccessProfiles, canAccessCaseType, canAccessCaseState);
            // SUCCESS for role 3
            RoleAssignment roleAssignment3 = filteredMatchingRoleAssignments.get(2);
            setupAccessGrantedCheckMocks(roleAssignment3, List.of("AG3"), true, true);
            // FAIL for role 4
            RoleAssignment roleAssignment4 = filteredMatchingRoleAssignments.get(3);
            setupAccessGrantedCheckMocks(roleAssignment4, outputAccessProfiles, canAccessCaseType, canAccessCaseState);

            setupAccessProfileCheckMocks_firstSucceeds(filteredMatchingRoleAssignments);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertNotNull(caseAccessMetadata);
            verifyNoInteractions(pseudoRoleAssignmentsGenerator);

            // only 2 grantTypes should have made it
            assertEquals(2, caseAccessMetadata.getAccessGrants().size());

            // check grant type for role 1 present
            GrantType grantType1 = GrantType.valueOf(roleAssignment1.getGrantType());
            assertTrue(caseAccessMetadata.getAccessGrants().contains(grantType1));
            // check grant type for role 2 NOT present
            GrantType grantType2 = GrantType.valueOf(roleAssignment2.getGrantType());
            assertFalse(caseAccessMetadata.getAccessGrants().contains(grantType2));
            // check grant type for role 3 present
            GrantType grantType3 = GrantType.valueOf(roleAssignment3.getGrantType());
            assertTrue(caseAccessMetadata.getAccessGrants().contains(grantType3));
            // check grant type for role 4 NOT present
            GrantType grantType4 = GrantType.valueOf(roleAssignment4.getGrantType());
            assertFalse(caseAccessMetadata.getAccessGrants().contains(grantType4));
        }

        @ParameterizedTest(
            name = "generateAccessMetadata_returnsAccessGrantedList_includingWhenAGChecksFailsThenPasses: {0}, {1}, {2}"
        )
        @MethodSource(ACCESS_GRANTED_CHECK_NEGATIVE_MATCH_PARAMS)
        void generateAccessMetadata_returnsAccessGrantedList_includingWhenAGChecksFailsThenPasses(
            List<String> outputAccessProfiles,
            boolean canAccessCaseType,
            boolean canAccessCaseState
        ) {

            // GIVEN
            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_1, STANDARD.name());
            roleAndGrantType.put(ROLE_NAME_2, STANDARD.name());
            setupStandardMocks(roleAndGrantType, false);

            // FAIL for role 1
            RoleAssignment roleAssignment1 = filteredMatchingRoleAssignments.get(0);
            setupAccessGrantedCheckMocks(roleAssignment1, outputAccessProfiles, canAccessCaseType, canAccessCaseState);
            // SUCCESS for role 2
            RoleAssignment roleAssignment2 = filteredMatchingRoleAssignments.get(1);
            setupAccessGrantedCheckMocks(roleAssignment2, List.of("AG2"), true, true);

            setupAccessProfileCheckMocks_firstSucceeds(filteredMatchingRoleAssignments);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertNotNull(caseAccessMetadata);
            verifyNoInteractions(pseudoRoleAssignmentsGenerator);

            // expecting only 1 grantType of type STANDARD (as failed once but also passed at least once)
            assertEquals(1, caseAccessMetadata.getAccessGrants().size());
            assertEquals(STANDARD, caseAccessMetadata.getAccessGrants().get(0));
        }

        @Test
        void generateAccessMetadata_returnsAccessProcessValueOf_none() {

            // GIVEN
            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_1, STANDARD.name());
            roleAndGrantType.put(ROLE_NAME_2, BASIC.name());
            setupStandardMocks(roleAndGrantType, false);

            setupAccessGrantedCheckMocks_allSuccess(filteredMatchingRoleAssignments);
            setupAccessProfileCheckMocks_firstSucceeds(filteredMatchingRoleAssignments);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertEquals(AccessProcess.NONE.name(), caseAccessMetadata.getAccessProcessString());
        }

        @ParameterizedTest(
            name = "generateAccessMetadata_returnsAccessProcessValueOf_challenged_whenFirstAPCheckFails: {0}, {1}, {2}"
        )
        @MethodSource(ACCESS_PROFILE_CHECK_NEGATIVE_MATCH_PARAMS)
        void generateAccessMetadata_returnsAccessProcessValueOf_challenged_whenFirstAPCheckFails(
            List<String> outputAccessProfiles,
            boolean canAccessCaseType,
            boolean canAccessCaseState
        ) {

            // GIVEN
            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_2, BASIC.name());
            roleAndGrantType.put(ROLE_NAME_3, BASIC.name());
            setupStandardMocks(roleAndGrantType, false);

            setupAccessGrantedCheckMocks_allSuccess(filteredMatchingRoleAssignments);
            // make the first AP check FAIL
            setupAccessProfileCheckMocks(
                filteredMatchingRoleAssignments, outputAccessProfiles, canAccessCaseType, canAccessCaseState
            );

            // make the second AP check SUCCEED
            setupSecondAccessProfileCheckMocks(List.of(ROLE_NAME_1), List.of("AP1"), true, true);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertEquals(AccessProcess.CHALLENGED.name(), caseAccessMetadata.getAccessProcessString());
        }

        @ParameterizedTest(
            name = "generateAccessMetadata_returnsAccessProcessValueOf_specific_whenBothAPChecksFail: {0}, {1}, {2}"
        )
        @MethodSource(ACCESS_PROFILE_CHECK_NEGATIVE_MATCH_PARAMS)
        void generateAccessMetadata_returnsAccessProcessValueOf_specific_whenBothAPChecksFail(
            List<String> outputAccessProfiles,
            boolean canAccessCaseType,
            boolean canAccessCaseState
        ) {

            // GIVEN
            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_2, STANDARD.name());
            roleAndGrantType.put(ROLE_NAME_3, STANDARD.name());
            setupStandardMocks(roleAndGrantType, false);

            setupAccessGrantedCheckMocks_allSuccess(filteredMatchingRoleAssignments);
            // make the first AP check FAIL
            setupAccessProfileCheckMocks(
                filteredMatchingRoleAssignments, outputAccessProfiles, canAccessCaseType, canAccessCaseState
            );

            // make the second AP check FAIL
            setupSecondAccessProfileCheckMocks(
                List.of(ROLE_NAME_1), outputAccessProfiles, canAccessCaseType, canAccessCaseState
            );

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertEquals(AccessProcess.SPECIFIC.name(), caseAccessMetadata.getAccessProcessString());
        }

        @ParameterizedTest(
            name = "generateAccessMetadata_returnsAccessProcessValueOf_specific"
                + "_whenNoRegionOrLocationRoleAssignmentsExist"
                + "_andFirstAPCheckFails: {0}, {1}, {2}"
        )
        @MethodSource(ACCESS_PROFILE_CHECK_NEGATIVE_MATCH_PARAMS)
        void generateAccessMetadata_returnsAccessProcessValueOf_specific_whenNoRegionOrLocationRoleAssignmentsExist(
            List<String> outputAccessProfiles,
            boolean canAccessCaseType,
            boolean canAccessCaseState
        ) {

            // GIVEN
            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_2, BASIC.name());
            roleAndGrantType.put(ROLE_NAME_3, BASIC.name());
            setupStandardMocks(roleAndGrantType, false);

            setupAccessGrantedCheckMocks_allSuccess(filteredMatchingRoleAssignments);
            // make the first AP check FAIL
            setupAccessProfileCheckMocks(
                filteredMatchingRoleAssignments, outputAccessProfiles, canAccessCaseType, canAccessCaseState
            );
            // mock when no failing region/location matches (i.e. skip the second check)
            doReturn(List.of())
                .when(filteredRoleAssignments).getFilteredRoleAssignmentsFailedOnRegionOrBaseLocationMatcher();

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertEquals(AccessProcess.SPECIFIC.name(), caseAccessMetadata.getAccessProcessString());

            AccessProfileServiceCalls apServiceCalls = verifyAccessProfileServiceCalls();

            // verify AccessProcess check lookup only once:
            //    i.e. second call skipped as no region or location RoleAssignments exist
            assertEquals(1, apServiceCalls.accessProcessChecks.size());
        }

        @Test
        void generateAccessMetadata_withPseudoRoleAssignmentsGeneration() {

            // GIVEN
            addPseudoRoleAssignments(List.of(ROLE_NAME_1)); // NB: these will be: grantType = STANDARD

            Map<String, String> roleAndGrantType = Maps.newHashMap();
            roleAndGrantType.put(ROLE_NAME_2, SPECIFIC.name());
            roleAndGrantType.put(ROLE_NAME_3, CHALLENGED.name());
            roleAndGrantType.put(ROLE_NAME_4, BASIC.name());
            setupStandardMocks(roleAndGrantType, true);

            List<RoleAssignment> filteredForAGChecksAndPseudoRolesAssignments = new ArrayList<>();
            filteredForAGChecksAndPseudoRolesAssignments.addAll(filteredMatchingRoleAssignments);
            filteredForAGChecksAndPseudoRolesAssignments.addAll(pseudoRoleAssignments);

            List<RoleAssignment> filteredForAPChecksAndPseudoRolesAssignments = new ArrayList<>();
            filteredForAPChecksAndPseudoRolesAssignments.addAll(
                // NB: must filter out BASIC here but will see it again in output: caseAccessMetadata.getAccessGrants()
                filterRoleAssignmentsForAccessProfileServiceCall(filteredMatchingRoleAssignments)
            );
            filteredForAPChecksAndPseudoRolesAssignments.addAll(pseudoRoleAssignments);

            setupAccessGrantedCheckMocks_allSuccess(filteredForAGChecksAndPseudoRolesAssignments);
            setupAccessProfileCheckMocks_firstSucceeds(filteredForAPChecksAndPseudoRolesAssignments);

            // WHEN
            CaseAccessMetadata caseAccessMetadata = defaultCaseDataAccessControl.generateAccessMetadata(CASE_ID);

            // THEN
            assertEquals(AccessProcess.NONE.name(), caseAccessMetadata.getAccessProcessString());

            // NB: must include all GrantTypes from original RoleAssignments (i.e. not filtered list) + Pseudo RAs
            List<GrantType> expectedAccessGrants = List.of(BASIC, CHALLENGED, SPECIFIC, STANDARD);
            List<GrantType> actualAccessGrants = caseAccessMetadata.getAccessGrants();
            assertTrue(expectedAccessGrants.size() == actualAccessGrants.size()
                && expectedAccessGrants.containsAll(actualAccessGrants));
        }

        private void setupStandardMocks(Map<String, String> roleAndGrantType,
                                        boolean enablePseudoRolesAssignmentGeneration) {
            doReturn(USER_ID).when(securityUtils).getUserId();

            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(anyString());

            doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_1);

            doReturn(usersRoleAssignments).when(roleAssignmentService).getRoleAssignments(USER_ID);

            doReturn(enablePseudoRolesAssignmentGeneration)
                .when(applicationParams).getEnablePseudoRoleAssignmentsGeneration();

            doReturn(filteredRoleAssignments)
                .when(roleAssignmentsFilteringService).filter(usersRoleAssignments, caseDetails);

            filteredMatchingRoleAssignments = createFilteringResults(roleAndGrantType);
            doReturn(filteredMatchingRoleAssignments)
                .when(filteredRoleAssignments).getFilteredMatchingRoleAssignments();

            if (enablePseudoRolesAssignmentGeneration) {
                doReturn(pseudoRoleAssignments).when(pseudoRoleAssignmentsGenerator)
                    .createPseudoRoleAssignments(filteredMatchingRoleAssignments, false);
            }
        }

        private void setupAccessGrantedCheckMocks_allSuccess(List<RoleAssignment> inputRoleAssignments) {
            List<RoleAssignment> roleAssignments
                = filterRoleAssignmentsForAccessGrantedServiceCall(inputRoleAssignments);

            for (int i = 0; i < roleAssignments.size(); i++) {
                setupAccessGrantedCheckMocks(roleAssignments.get(i), List.of("AG" + (i + 1)), true, true);
            }
        }

        private void setupAccessGrantedCheckMocks(RoleAssignment roleAssignment,
                                                List<String> outputAccessProfiles,
                                                boolean canAccessCaseType,
                                                boolean canAccessCaseState) {

            // NB: same mocks as AP checks but each RA checked separately
            setupAccessProfileCheckMocks(
                List.of(roleAssignment),
                outputAccessProfiles,
                canAccessCaseType,
                canAccessCaseState
            );
        }

        private void setupAccessProfileCheckMocks_firstSucceeds(List<RoleAssignment> inputRoleAssignments) {
            setupAccessProfileCheckMocks(inputRoleAssignments, List.of("AP1", "AP2"), true, true);
        }

        private void setupAccessProfileCheckMocks(List<RoleAssignment> inputRoleAssignments,
                                                  List<String> outputAccessProfiles,
                                                  boolean canAccessCaseType,
                                                  boolean canAccessCaseState) {

            List<AccessProfile> accessProfileList = outputAccessProfiles.stream()
                .map(ap -> AccessProfile.builder().accessProfile(ap).build())
                .collect(Collectors.toList());
            Set<AccessProfile> accessProfileSet = new HashSet<>(accessProfileList);

            doReturn(accessProfileList).when(accessProfileService).generateAccessProfiles(
                argThat(arg -> arg.size() == inputRoleAssignments.size() && arg.containsAll(inputRoleAssignments)),
                any()
            );

            doReturn(canAccessCaseType)
                .when(accessControlService)
                .canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfileSet, CAN_READ);

            doReturn(canAccessCaseState)
                .when(accessControlService)
                .canAccessCaseStateWithCriteria(CASE_STATE_1, caseTypeDefinition, accessProfileSet, CAN_READ);
        }

        private List<RoleAssignment> setupSecondAccessProfileCheckMocks(List<String> roleNames,
                                                                        List<String> outputAccessProfiles,
                                                                        boolean canAccessCaseType,
                                                                        boolean canAccessCaseState) {

            List<RoleAssignment> roleAssignments = roleNames.stream()
                    .map(value -> RoleAssignment.builder()
                        .roleName(ROLE_NAME_1)
                        .grantType(STANDARD.name())
                        .attributes(RoleAssignmentAttributes.builder().region(Optional.of("123")).build())
                        .build()
                    ).collect(Collectors.toList());

            doReturn(roleAssignments)
                .when(filteredRoleAssignments).getFilteredRoleAssignmentsFailedOnRegionOrBaseLocationMatcher();

            setupAccessProfileCheckMocks(roleAssignments, outputAccessProfiles, canAccessCaseType, canAccessCaseState);

            return roleAssignments;
        }

        private void addPseudoRoleAssignments(List<String> roleNames) {
            roleNames.forEach(roleName ->
                pseudoRoleAssignments.add(RoleAssignment.builder()
                    .roleName(AccessControl.IDAM_PREFIX + roleName)
                    .grantType(STANDARD.name())
                    .build()
                )
            );
        }

        private List<RoleAssignment> filterRoleAssignments(List<RoleAssignment> roleAssignments,
                                                           List<GrantType> grantTypeFilter) {
            List<String> grantTypeString = grantTypeFilter.stream().map(GrantType::name).collect(Collectors.toList());
            return  roleAssignments.stream()
                .filter(roleAssignment -> grantTypeString.contains(roleAssignment.getGrantType())
                ).collect(Collectors.toList());
        }

        private List<RoleAssignment> filterRoleAssignmentsForAccessGrantedServiceCall(
            List<RoleAssignment> roleAssignments
        ) {
            return  filterRoleAssignments(roleAssignments, List.of(BASIC, STANDARD, SPECIFIC, CHALLENGED));
        }

        private List<RoleAssignment> filterRoleAssignmentsForAccessProfileServiceCall(
            List<RoleAssignment> roleAssignments
        ) {
            return filterRoleAssignments(roleAssignments, List.of(STANDARD, SPECIFIC, CHALLENGED));
        }

        private int numberOfExpectedGenerateAccessProfileCalls(List<RoleAssignment> roleAssignments) {
            // number of AG calls + number of AP calls (usually 1)
            return filterRoleAssignmentsForAccessGrantedServiceCall(roleAssignments).size() + 1;
        }

        private AccessProfileServiceCalls verifyAccessProfileServiceCalls() {
            verify(accessProfileService, atLeast(1)).generateAccessProfiles(roleAssignmentListCaptor.capture(), any());
            List<List<RoleAssignment>> roleAssignmentListValues = roleAssignmentListCaptor.getAllValues();
            List<List<RoleAssignment>> accessGrantedChecks = new ArrayList<>();
            List<List<RoleAssignment>> accessProcessChecks = new ArrayList<>();

            boolean searchingForAccessGrantedCalls = true;
            // Sort calls in to AccessGrant and AccessProcess checks:
            //    The AG calls will come first and will always be for singular RoleAssignments (i.e. size() = 1)
            //    NB: Our test data will always use multiple RAs so we have a safe cut-over once size() > 1.
            for (List<RoleAssignment> roleAssignmentList : roleAssignmentListValues) {
                if (searchingForAccessGrantedCalls && roleAssignmentList.size() == 1) {
                    accessGrantedChecks.add(roleAssignmentList);
                } else {
                    searchingForAccessGrantedCalls = false;
                    accessProcessChecks.add(roleAssignmentList);
                }
            }

            return AccessProfileServiceCalls.builder()
                .accessGrantedChecks(accessGrantedChecks)
                .accessProcessChecks(accessProcessChecks)
                .build();
        }
    }


    private Map<String, JsonNode> generateCaseData(final String value)
        throws JsonProcessingException {
        final String caseData = (value == null) ? "{}"
            : "{\n" + "\"CaseAccessCategory\": \"" + value + "\"\n" + "  }";
        return JacksonUtils.convertValue(mapper.readTree(caseData));
    }

    private static final String ACCESS_GRANTED_CHECK_NEGATIVE_MATCH_PARAMS
        = "uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.DefaultCaseDataAccessControlTest"
        + "#accessGrantedCheckNegativeMatchParams";

    @SuppressWarnings("unused")
    private static Stream<Arguments> accessGrantedCheckNegativeMatchParams() {
        return Stream.of(
            // NB: params correspond to:
            // * outputAccessProfiles :: mock output for accessProfileService.generateAccessProfiles(...)
            // * canAccessCaseType :: mock output for accessControlService.canAccessCaseTypeWithCriteria(...)
            // * canAccessCaseState :: mock output for accessControlService.canAccessCaseStateWithCriteria(...)
            Arguments.of(List.of(), true, true),
            Arguments.of(List.of("AG1", "AG2"), false, true),
            Arguments.of(List.of("AG1", "AG2"), true, false)
        );
    }

    private static final String ACCESS_PROFILE_CHECK_NEGATIVE_MATCH_PARAMS
        = "uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.DefaultCaseDataAccessControlTest"
        + "#accessProfileCheckNegativeMatchParams";

    @SuppressWarnings("unused")
    private static Stream<Arguments> accessProfileCheckNegativeMatchParams() {
        return Stream.of(
            // NB: params correspond to:
            // * outputAccessProfiles :: mock output for accessProfileService.generateAccessProfiles(...)
            // * canAccessCaseType :: mock output for accessControlService.canAccessCaseTypeWithCriteria(...)
            // * canAccessCaseState :: mock output for accessControlService.canAccessCaseStateWithCriteria(...)
            Arguments.of(List.of(), true, true),
            Arguments.of(List.of("AP1"), false, true),
            Arguments.of(List.of("AP2"), true, false)
        );
    }

}
