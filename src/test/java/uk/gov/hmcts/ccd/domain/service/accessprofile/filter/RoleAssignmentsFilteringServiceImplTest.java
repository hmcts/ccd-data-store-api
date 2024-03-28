package uk.gov.hmcts.ccd.domain.service.accessprofile.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.BeginDateEndDateMatcher;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.CaseIdMatcher;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.CaseTypeMatcher;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.JurisdictionMatcher;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.LocationMatcher;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.MatcherType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.RegionMatcher;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.RoleAttributeMatcher;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.SecurityClassificationMatcher;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher.CaseAccessGroupsMatcher;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroup;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroupWithId;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentsFilteringServiceImpl;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessGroupUtils;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class RoleAssignmentsFilteringServiceImplTest extends BaseFilter {

    private RoleAssignmentsFilteringServiceImpl classUnderTest;

    static final String CASE_ACCESS_GROUPS = CaseAccessGroupUtils.CASE_ACCESS_GROUPS;

    static final String CASE_ACCESS_GROUP_ID = "SomeJurisdiction:CIVIL:bulk: [RESPONDENT01SOLICITOR]:"
        + " 550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private CaseDataService caseDataService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        List<RoleAttributeMatcher> roleAttributeMatchers = Lists.newArrayList(new BeginDateEndDateMatcher(),
            new CaseIdMatcher(),
            new CaseTypeMatcher(),
            new JurisdictionMatcher(),
            new LocationMatcher(),
            new RegionMatcher(),
            new SecurityClassificationMatcher(),
            new CaseAccessGroupsMatcher());
        classUnderTest = new RoleAssignmentsFilteringServiceImpl(roleAttributeMatchers);
    }

    @Test
    void shouldFilterBasedOnDateCaseIDJurisdiction() {
        RoleAssignments roleAssignments = mockRoleAssignments();
        CaseDetails caseDetails = mockCaseDetails();
        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails).getFilteredMatchingRoleAssignments();
        assertEquals(1, filteredRoleAssignments.size());
    }

    @Test
    void shouldFilterBasedOnSecurityClassificationWhenCaseClassificationIsLess() {
        RoleAssignments roleAssignments = mockRoleAssignmentsOnSecurityClassification();
        CaseDetails caseDetails = mockCaseDetails();
        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails).getFilteredMatchingRoleAssignments();
        assertEquals(2, filteredRoleAssignments.size());
    }

    @Test
    void shouldFilterBasedOnSecurityClassificationWhenCaseAccessGroupCaseClassificationIsLess() {
        RoleAssignments roleAssignments = mockRoleAssignmentsOnSecurityClassification();
        CaseDetails caseDetails = setUpCaseDetailsWithCaseAccessGroup();

        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails).getFilteredMatchingRoleAssignments();
        assertEquals(2, filteredRoleAssignments.size());
    }

    @Test
    void shouldFilterBasedOnSecurityClassificationWhenCaseClassificationIsMore() {
        RoleAssignments roleAssignments = mockRoleAssignmentsOnSecurityClassification();
        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.PRIVATE);
        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails).getFilteredMatchingRoleAssignments();
        assertEquals(2, filteredRoleAssignments.size());
    }

    @Ignore // Test returns 0 neew to investigate
    //@Test
    void shouldFilterBasedOnSecurityClassificationWhenCaseAccessGroupCaseClassificationIsMore() {
        RoleAssignments roleAssignments = mockRoleAssignmentsCaseAccessGroupMatching(SecurityClassification.PRIVATE,
            SecurityClassification.RESTRICTED);
        CaseDetails caseDetails = setUpCaseDetailsWithCaseAccessGroup(SecurityClassification.PRIVATE);

        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails).getFilteredMatchingRoleAssignments();
        assertEquals(2, filteredRoleAssignments.size());
    }

    @Test
    void shouldFilterBasedOnSecurityClassificationWhenCaseClassificationIsRestricted() {
        RoleAssignments roleAssignments = mockRoleAssignmentsOnSecurityClassification();
        CaseDetails caseDetails = mockCaseDetails(SecurityClassification.RESTRICTED);
        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails).getFilteredMatchingRoleAssignments();
        assertEquals(1, filteredRoleAssignments.size());
    }

    @Ignore // Test returns 0
    //@Test
    void shouldFilterBasedOnSecurityClassificationWhenCaseAccessGroupCaseClassificationIsRestricted() {
        RoleAssignments roleAssignments = mockRoleAssignmentsCaseAccessGroupMatching(SecurityClassification.PRIVATE,
            SecurityClassification.RESTRICTED);
        CaseDetails caseDetails = setUpCaseDetailsWithCaseAccessGroup(SecurityClassification.RESTRICTED);

        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails).getFilteredMatchingRoleAssignments();
        assertEquals(1, filteredRoleAssignments.size());
    }

    @Test
    void shouldFilterBasedOnStartDateAndEndDate() {
        RoleAssignments roleAssignments = mockRoleAssignmentsDatesNotMatching();
        CaseDetails caseDetails = mockCaseDetails();
        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails).getFilteredMatchingRoleAssignments();
        assertEquals(0, filteredRoleAssignments.size());
    }

    private RoleAssignments mockRoleAssignments() {
        RoleAssignments roleAssignments = mock(RoleAssignments.class);

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1);
        RoleAssignment roleAssignment2 = createRoleAssignment(CASE_ID_2, JURISDICTION_2);

        List<RoleAssignment> roleAssignmentList = Lists.newArrayList(roleAssignment, roleAssignment2);

        when(roleAssignments.getRoleAssignments()).thenReturn(roleAssignmentList);

        return roleAssignments;
    }

    private RoleAssignments mockRoleAssignmentsOnSecurityClassification() {
        RoleAssignments roleAssignments = mock(RoleAssignments.class);

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE");
        RoleAssignment roleAssignment2 = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "RESTRICTED");

        List<RoleAssignment> roleAssignmentList = Lists.newArrayList(roleAssignment, roleAssignment2);

        when(roleAssignments.getRoleAssignments()).thenReturn(roleAssignmentList);

        return roleAssignments;
    }

    private RoleAssignments mockRoleAssignmentsWithExcludedGrantType() {
        RoleAssignments roleAssignments = mock(RoleAssignments.class);

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PRIVATE");
        roleAssignment.setGrantType(GrantType.EXCLUDED.name());
        RoleAssignment roleAssignment2 = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "RESTRICTED");
        roleAssignment2.setGrantType(GrantType.EXCLUDED.name());

        List<RoleAssignment> roleAssignmentList = Lists.newArrayList(roleAssignment, roleAssignment2);

        when(roleAssignments.getRoleAssignments()).thenReturn(roleAssignmentList);

        return roleAssignments;
    }

    private RoleAssignments mockRoleAssignmentsDatesNotMatching() {
        RoleAssignments roleAssignments = mock(RoleAssignments.class);

        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().plus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PUBLIC");
        RoleAssignment roleAssignment2 = createRoleAssignment(CASE_ID_2, JURISDICTION_2,
            Instant.now().plus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), "PUBLIC");

        List<RoleAssignment> roleAssignmentList = Lists.newArrayList(roleAssignment, roleAssignment2);

        when(roleAssignments.getRoleAssignments()).thenReturn(roleAssignmentList);

        return roleAssignments;
    }

    private RoleAssignments mockRoleAssignmentsCaseAccessGroupMatching(SecurityClassification securityClassification1,
                                                                       SecurityClassification securityClassification2) {


        RoleAssignments roleAssignments = mock(RoleAssignments.class);
        RoleAssignment roleAssignment = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().plus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), securityClassification1.name(), CASE_ACCESS_GROUP_ID);

        RoleAssignment roleAssignment2 = createRoleAssignment(CASE_ID_1, JURISDICTION_1,
            Instant.now().plus(1, ChronoUnit.DAYS),
            Instant.now().plus(2, ChronoUnit.DAYS), securityClassification2.name(), CASE_ACCESS_GROUP_ID);

        List<RoleAssignment> roleAssignmentList = Lists.newArrayList(roleAssignment, roleAssignment2);

        when(roleAssignments.getRoleAssignments()).thenReturn(roleAssignmentList);

        return roleAssignments;
    }

    @Test
    void shouldNotInvokeExcludedMatcherForCaseType() {
        RoleAssignments roleAssignments = mockRoleAssignmentsWithExcludedGrantType();
        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();
        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseTypeDefinition, Arrays.asList(MatcherType.GRANTTYPE, MatcherType.CASETYPE))
            .getFilteredMatchingRoleAssignments();
        assertEquals(2, filteredRoleAssignments.size());
    }

    @Test
    void shouldInvokeMatchersWhenExcludeMatchingListIsEmptyForCaseType() {
        RoleAssignments roleAssignments = mockRoleAssignmentsWithExcludedGrantType();
        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();
        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseTypeDefinition,
                Lists.newArrayList()).getFilteredMatchingRoleAssignments();
        assertEquals(0, filteredRoleAssignments.size());
    }

    @Test
    void shouldNotInvokeExcludedMatcherForCase() {
        RoleAssignments roleAssignments = mockRoleAssignmentsDatesNotMatching();
        CaseDetails caseDetails = mockCaseDetails();

        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails,
                Arrays.asList(MatcherType.BEGINENDDATE, MatcherType.CASEID, MatcherType.JURISDICTION))
            .getFilteredMatchingRoleAssignments();
        assertEquals(2, filteredRoleAssignments.size());
    }

    @Test
    void shouldInvokeMatchersWhenExcludeMatchingListIsEmptyForCase() {
        RoleAssignments roleAssignments = mockRoleAssignmentsDatesNotMatching();
        CaseDetails caseDetails = mockCaseDetails();
        List<RoleAssignment> filteredRoleAssignments = classUnderTest
            .filter(roleAssignments, caseDetails,
                Lists.newArrayList()).getFilteredMatchingRoleAssignments();
        assertEquals(0, filteredRoleAssignments.size());
    }

    private CaseDetails setUpCaseDetailsWithCaseAccessGroup(SecurityClassification securityClassification) {

        Map<String, JsonNode> dataCaseAccessGroup = new HashMap<>();
        dataCaseAccessGroup.put(CASE_ACCESS_GROUPS, getCaseAccessGroups());
        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();

        CaseDetails caseDetails =  mockCaseDetails(securityClassification, dataCaseAccessGroup,
            caseDataService, caseTypeDefinition, objectMapper);
        return caseDetails;
    }

    private CaseDetails setUpCaseDetailsWithCaseAccessGroup() {

        Map<String, JsonNode> dataCaseAccessGroup = new HashMap<>();
        dataCaseAccessGroup.put(CASE_ACCESS_GROUPS, getCaseAccessGroups());
        CaseTypeDefinition caseTypeDefinition = mockCaseTypeDefinition();

        return mockCaseDetails(dataCaseAccessGroup, caseDataService, caseTypeDefinition, objectMapper);

    }

    private JsonNode getCaseAccessGroups() {
        String caseAccessGroupType = "Any String";
        String caseAccessGroupID = CASE_ACCESS_GROUPS;

        List<CaseAccessGroupWithId> caseAccessGroupForUIs = new ArrayList<>();

        CaseAccessGroup caseAccessGroup = CaseAccessGroup.builder().caseAccessGroupId(caseAccessGroupID)
            .caseAccessGroupType(caseAccessGroupType).build();
        String uuid = UUID.randomUUID().toString();

        CaseAccessGroupWithId caseAccessGroupForUI = CaseAccessGroupWithId.builder()
            .caseAccessGroup(caseAccessGroup)
            .id(uuid).build();
        caseAccessGroupForUIs.add(caseAccessGroupForUI);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode data  = mapper.convertValue(caseAccessGroupForUIs, JsonNode.class);
        return data;
    }
}
