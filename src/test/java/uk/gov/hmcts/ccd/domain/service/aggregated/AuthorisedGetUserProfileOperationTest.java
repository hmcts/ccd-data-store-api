package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.User;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class AuthorisedGetUserProfileOperationTest {
    private final UserProfile userProfile = new UserProfile();
    private final User user = new User();
    private final JurisdictionDisplayProperties test1JurisdictionDisplayProperties =
        new JurisdictionDisplayProperties();
    private final JurisdictionDisplayProperties test2JurisdictionDisplayProperties =
        new JurisdictionDisplayProperties();

    private Set<String> userRoles = Sets.newHashSet("role1", "role2", "role3");
    private Set<AccessProfile> accessProfiles = createAccessProfiles(userRoles);

    private List<CaseStateDefinition> caseStateDefinitions =
        Arrays.asList(new CaseStateDefinition(), new CaseStateDefinition(), new CaseStateDefinition());
    private List<CaseEventDefinition> caseEventDefinitions = Arrays.asList(new CaseEventDefinition(),
        new CaseEventDefinition(),
        new CaseEventDefinition(),
        new CaseEventDefinition());

    private CaseTypeDefinition notAllowedCaseTypeDefinition = new CaseTypeDefinition();

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private GetUserProfileOperation getUserProfileOperation;

    private AuthorisedGetUserProfileOperation classUnderTest;

    @Mock
    private CaseUserRepository caseUserRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userProfile.setUser(user);
        userProfile.setJurisdictions(new JurisdictionDisplayProperties[]{test1JurisdictionDisplayProperties,
            test2JurisdictionDisplayProperties});

        List<CaseTypeDefinition> caseTypes1Definition =
            Arrays.asList(notAllowedCaseTypeDefinition, new CaseTypeDefinition());
        List<CaseTypeDefinition> caseTypes2Definition =
            Arrays.asList(new CaseTypeDefinition(), notAllowedCaseTypeDefinition, new CaseTypeDefinition());

        test1JurisdictionDisplayProperties.setCaseTypeDefinitions(caseTypes1Definition);
        test2JurisdictionDisplayProperties.setCaseTypeDefinitions(caseTypes2Definition);

        when(caseDataAccessControl.generateOrganisationalAccessProfilesByCaseTypeId(any()))
            .thenReturn(accessProfiles);
        doReturn(userProfile).when(getUserProfileOperation).execute(CAN_READ);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(any(), any(), any());
        classUnderTest =
            new AuthorisedGetUserProfileOperation(accessControlService, getUserProfileOperation, caseDataAccessControl);
    }

    private Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
    }


    @Test
    @DisplayName("should return only caseTypes the user is allowed to access")
    public void execute() {
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(eq(notAllowedCaseTypeDefinition),
            eq(accessProfiles), eq(CAN_READ));
        doReturn(caseStateDefinitions).when(accessControlService)
            .filterCaseStatesByAccess(any(CaseTypeDefinition.class), eq(accessProfiles), eq(CAN_READ));
        doReturn(caseEventDefinitions).when(accessControlService).filterCaseEventsByAccess(any(), eq(accessProfiles),
            eq(CAN_READ));

        UserProfile userProfile = classUnderTest.execute(CAN_READ);

        assertAll(
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypeDefinitions().size(), is(2)),
            () -> assertThat(userProfile.getJurisdictions()[1].getCaseTypeDefinitions().size(), is(3)),
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypeDefinitions(), everyItem(not(isIn(Arrays
                .asList(notAllowedCaseTypeDefinition))))),
            () -> assertThat(userProfile.getJurisdictions()[1].getCaseTypeDefinitions(), everyItem(not(isIn(Arrays
                .asList(notAllowedCaseTypeDefinition))))),
            () ->
                assertThat(userProfile.getJurisdictions()[0].getCaseTypeDefinitions().get(0).getStates().size(), is(3)),
            () ->
                assertThat(userProfile.getJurisdictions()[0].getCaseTypeDefinitions().get(0).getEvents().size(), is(4))
        );
    }

    @Test
    @DisplayName("should return UserProfile for create organisation")
    public void executeForCreate() {
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(eq(notAllowedCaseTypeDefinition),
            eq(accessProfiles), eq(CAN_CREATE));
        doReturn(caseStateDefinitions).when(accessControlService)
            .filterCaseStatesByAccess(any(CaseTypeDefinition.class), eq(accessProfiles), eq(CAN_CREATE));
        doReturn(caseEventDefinitions).when(accessControlService).filterCaseEventsByAccess(any(), eq(accessProfiles),
            eq(CAN_CREATE));
        doReturn(userProfile).when(getUserProfileOperation).execute(CAN_CREATE);

        UserProfile userProfile = classUnderTest.execute(CAN_CREATE);

        assertAll(
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypeDefinitions().size(), is(2)),
            () -> assertThat(userProfile.getJurisdictions()[1].getCaseTypeDefinitions().size(), is(3)),
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypeDefinitions(), everyItem(not(isIn(Arrays
                .asList(notAllowedCaseTypeDefinition))))),
            () -> assertThat(userProfile.getJurisdictions()[1].getCaseTypeDefinitions(), everyItem(not(isIn(Arrays
                .asList(notAllowedCaseTypeDefinition))))),
            () ->
                assertThat(userProfile.getJurisdictions()[0].getCaseTypeDefinitions().get(0).getStates().size(), is(3)),
            () ->
                assertThat(userProfile.getJurisdictions()[0].getCaseTypeDefinitions().get(0).getEvents().size(), is(4))
        );
    }


    @Test
    @DisplayName("should return UserProfile for create non organisation")
    public void executeForCreateWithNonOrganisationProfile() {
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(eq(notAllowedCaseTypeDefinition),
            eq(accessProfiles), eq(CAN_CREATE));
        doReturn(caseStateDefinitions).when(accessControlService)
            .filterCaseStatesByAccess(any(CaseTypeDefinition.class), eq(accessProfiles), eq(CAN_CREATE));
        doReturn(caseEventDefinitions).when(accessControlService).filterCaseEventsByAccess(any(), eq(accessProfiles),
            eq(CAN_CREATE));
        doReturn(userProfile).when(getUserProfileOperation).execute(CAN_CREATE);
        doReturn(true).when(caseDataAccessControl).shouldRemoveCaseDefinition(eq(accessProfiles),eq(CAN_CREATE),any());

        UserProfile userProfile = classUnderTest.execute(CAN_CREATE);

        assertAll(
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypeDefinitions().size(), is(0)),
            () -> assertThat(userProfile.getJurisdictions()[1].getCaseTypeDefinitions().size(), is(0)),
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypeDefinitions(), everyItem(not(isIn(Arrays
                .asList(notAllowedCaseTypeDefinition))))),
            () -> assertThat(userProfile.getJurisdictions()[1].getCaseTypeDefinitions(), everyItem(not(isIn(Arrays
                .asList(notAllowedCaseTypeDefinition)))))
        );
    }


    @Test
    @DisplayName("should return empty caseType if the user is not allowed to access any case type")
    void shouldReturnEmptyCaseType() {
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(any(),
            eq(accessProfiles), eq(CAN_READ));

        UserProfile userProfile = classUnderTest.execute(CAN_READ);

        assertAll(
            () -> assertThat(userProfile.getJurisdictions()[0].getCaseTypeDefinitions().size(), is(0)),
            () -> assertThat(userProfile.getJurisdictions()[1].getCaseTypeDefinitions().size(), is(0))
        );
    }


    @Test
    @DisplayName("should return empty jurisdictions if the user is not allowed to access any case type")
    void shouldReturnEmptyJurisdictions() {
        userProfile.setJurisdictions(new JurisdictionDisplayProperties[0]);
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(any(),
            eq(accessProfiles), eq(CAN_READ));

        UserProfile userProfile = classUnderTest.execute(CAN_READ);

        assertAll(
            () -> assertThat(userProfile.getJurisdictions().length, is(0))
        );
    }
}
