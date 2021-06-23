package uk.gov.hmcts.ccd.data.user;

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.JurisdictionMapper;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.aggregated.WorkbasketDefault;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class UserServiceTest {

    @Mock
    private uk.gov.hmcts.ccd.data.user.UserRepository userRepoMock;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepoMock;
    @Mock
    private JurisdictionMapper jurisdictionMapperMock;
    @Mock
    private IdamProperties mockIdamProps;
    @Mock
    private JurisdictionDisplayProperties jdp1;
    @Mock
    private JurisdictionDisplayProperties jdp2;
    @Mock
    private JurisdictionDisplayProperties jdp3;
    @Mock
    private uk.gov.hmcts.ccd.data.user.JurisdictionsResolver jurisdictionsResolver;

    private JurisdictionDefinition j1;
    private JurisdictionDefinition j2;
    private JurisdictionDefinition unknownJurisdictionDefinition;
    private uk.gov.hmcts.ccd.data.user.UserService userService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        userService = new uk.gov.hmcts.ccd.data.user.UserService(userRepoMock, caseDefinitionRepoMock,
                jurisdictionMapperMock,jurisdictionsResolver);
        when(mockIdamProps.getEmail()).thenReturn("email");
        initialiseJurisdictions();
    }

    @Test
    public void testReturnsUserProfileDiscardingUnknownJurisdictions() {
        when(userRepoMock.getUserDetails()).thenReturn(mockIdamProps);
        UserDefault userDefaultMock = userDefault();
        when(userRepoMock.getUserDefaultSettings("email")).thenReturn(userDefaultMock);
        when(jurisdictionsResolver.getJurisdictions()).thenReturn(Lists.newArrayList("J1", "J2", "J3"));
        when(caseDefinitionRepoMock.getJurisdictions(Arrays.asList("J1", "J2", "J3")))
            .thenReturn(Arrays.asList(j1, j2, unknownJurisdictionDefinition));
        when(jurisdictionMapperMock.toResponse(j1)).thenReturn(jdp1);
        when(jurisdictionMapperMock.toResponse(j2)).thenReturn(jdp2);
        when(jurisdictionMapperMock.toResponse(unknownJurisdictionDefinition)).thenReturn(jdp3);

        UserProfile userProfile = userService.getUserProfile();

        assertEquals(mockIdamProps, userProfile.getUser().getIdamProperties());
        assertThat(userProfile.getJurisdictions(),
            equalTo(new JurisdictionDisplayProperties[] { jdp1, jdp2, jdp3 }));
        WorkbasketDefault workbasketDefault = userProfile.getDefaultSettings().getWorkbasketDefault();
        assertEquals("J1", workbasketDefault.getJurisdictionId());
        assertEquals("CT", workbasketDefault.getCaseTypeId());
        assertEquals("ST", workbasketDefault.getStateId());
    }

    @Test
    public void testReturnsUserProfileNoWorkBasketDefaults() {
        when(userRepoMock.getUserDetails()).thenReturn(mockIdamProps);
        when(userRepoMock.getUserDefaultSettings("email"))
            .thenThrow(new ResourceNotFoundException("No User profile exists for this userId email"));
        when(jurisdictionsResolver.getJurisdictions()).thenReturn(Lists.newArrayList("J1", "J2", "J3"));
        when(caseDefinitionRepoMock.getJurisdictions(Arrays.asList("J1", "J2", "J3")))
            .thenReturn(Arrays.asList(j1, j2, unknownJurisdictionDefinition));
        when(jurisdictionMapperMock.toResponse(j1)).thenReturn(jdp1);
        when(jurisdictionMapperMock.toResponse(j2)).thenReturn(jdp2);
        when(jurisdictionMapperMock.toResponse(unknownJurisdictionDefinition)).thenReturn(jdp3);

        UserProfile userProfile = userService.getUserProfile();

        assertEquals(mockIdamProps, userProfile.getUser().getIdamProperties());
        assertThat(userProfile.getJurisdictions(),
            equalTo(new JurisdictionDisplayProperties[] { jdp1, jdp2, jdp3 }));
        WorkbasketDefault workbasketDefault = userProfile.getDefaultSettings().getWorkbasketDefault();
        assertNull(workbasketDefault);
    }

    @Test
    public void testReturnsUserProfileWithEmptyJurisdictions() {
        when(userRepoMock.getUserDetails()).thenReturn(mockIdamProps);
        UserDefault userDefaultMock = userDefault();
        when(userRepoMock.getUserDefaultSettings("email")).thenReturn(userDefaultMock);
        when(jurisdictionsResolver.getJurisdictions()).thenReturn(null);

        UserProfile userProfile = userService.getUserProfile();

        assertEquals(mockIdamProps, userProfile.getUser().getIdamProperties());
        assertThat(userProfile.getJurisdictions(),
            equalTo(new JurisdictionDisplayProperties[] { }));
    }

    private UserDefault userDefault() {
        UserDefault userDefault = new UserDefault();
        userDefault.setWorkBasketDefaultJurisdiction("J1");
        userDefault.setWorkBasketDefaultCaseType("CT");
        userDefault.setWorkBasketDefaultState("ST");
        List<JurisdictionDefinition> userJurisdictionDefinitions = newArrayList(j1, j2, unknownJurisdictionDefinition);
        userDefault.setJurisdictionDefinitions(userJurisdictionDefinitions);
        return userDefault;
    }

    private void initialiseJurisdictions() {
        j1 = new JurisdictionDefinition();
        j1.setId("J1");
        j1.setName("J1Name");
        j1.setDescription("Desc1");
        j2 = new JurisdictionDefinition();
        j2.setId("J2");
        j2.setName("J2Name");
        j2.setDescription("Desc2");
        unknownJurisdictionDefinition = new JurisdictionDefinition();
        unknownJurisdictionDefinition.setId("J3");
    }
}
