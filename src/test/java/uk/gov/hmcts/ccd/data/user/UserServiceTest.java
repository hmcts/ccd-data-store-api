package uk.gov.hmcts.ccd.data.user;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.JurisdictionMapper;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.aggregated.WorkbasketDefault;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class UserServiceTest {

    @Mock
    private UserRepository userRepoMock;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepoMock;
    @Mock
    private JurisdictionMapper jurisdictionMapperMock;
    @Mock
    private IDAMProperties mockIDAMProps;
    @Mock
    private JurisdictionDisplayProperties jdp1;
    @Mock
    private JurisdictionDisplayProperties jdp2;
    @Mock
    private JurisdictionDisplayProperties jdp3;
    @Mock
    private JurisdictionsResolver jurisdictionsResolver;

    private Jurisdiction j1;
    private Jurisdiction j2;
    private Jurisdiction unknownJurisdiction;
    private UserService userService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        userService = new UserService(userRepoMock, caseDefinitionRepoMock, jurisdictionMapperMock,jurisdictionsResolver);
        when(mockIDAMProps.getEmail()).thenReturn("email");
        initialiseJurisdictions();
    }

    @Test
    public void testReturnsUserProfileDiscardingUnknownJurisdictions() {

        when(userRepoMock.getUserDetails()).thenReturn(mockIDAMProps);
        UserDefault userDefaultMock = aUserDefault();
        when(userRepoMock.getUserDefaultSettings("email")).thenReturn(userDefaultMock);
        when(jurisdictionsResolver.getJurisdictions()).thenReturn(Lists.newArrayList("J1", "J2", "J3"));
        when(caseDefinitionRepoMock.getJurisdiction("J1")).thenReturn(j1);
        when(caseDefinitionRepoMock.getJurisdiction("J2")).thenReturn(j2);
        when(caseDefinitionRepoMock.getJurisdiction("J3")).thenReturn(unknownJurisdiction);
        when(jurisdictionMapperMock.toResponse(j1)).thenReturn(jdp1);
        when(jurisdictionMapperMock.toResponse(j2)).thenReturn(jdp2);
        when(jurisdictionMapperMock.toResponse(unknownJurisdiction)).thenReturn(jdp3);

        UserProfile userProfile = userService.getUserProfile();

        assertThat(userProfile.getUser().getIdamProperties(), is(mockIDAMProps));
        assertThat(userProfile.getJurisdictions(),
                equalTo(new JurisdictionDisplayProperties[] { jdp1, jdp2, jdp3 }));
        WorkbasketDefault workbasketDefault = userProfile.getDefaultSettings().getWorkbasketDefault();
        assertThat(workbasketDefault.getJurisdictionId(), is("J1"));
        assertThat(workbasketDefault.getCaseTypeId(), is("CT"));
        assertThat(workbasketDefault.getStateId(), is("ST"));
    }

    @Test
    public void testReturnsUserProfileNoWorkBasketDefaults() {

        when(userRepoMock.getUserDetails()).thenReturn(mockIDAMProps);
        when(userRepoMock.getUserDefaultSettings("email"))
            .thenThrow(new ResourceNotFoundException("No User profile exists for this userId email"));
        when(jurisdictionsResolver.getJurisdictions()).thenReturn(Lists.newArrayList("J1", "J2", "J3"));
        when(caseDefinitionRepoMock.getJurisdiction("J1")).thenReturn(j1);
        when(caseDefinitionRepoMock.getJurisdiction("J2")).thenReturn(j2);
        when(caseDefinitionRepoMock.getJurisdiction("J3")).thenReturn(unknownJurisdiction);
        when(jurisdictionMapperMock.toResponse(j1)).thenReturn(jdp1);
        when(jurisdictionMapperMock.toResponse(j2)).thenReturn(jdp2);
        when(jurisdictionMapperMock.toResponse(unknownJurisdiction)).thenReturn(jdp3);

        UserProfile userProfile = userService.getUserProfile();

        assertThat(userProfile.getUser().getIdamProperties(), is(mockIDAMProps));
        assertThat(userProfile.getJurisdictions(),
            equalTo(new JurisdictionDisplayProperties[] { jdp1, jdp2, jdp3 }));
        WorkbasketDefault workbasketDefault = userProfile.getDefaultSettings().getWorkbasketDefault();
        assertNull(workbasketDefault);
    }

    private UserDefault aUserDefault() {
        UserDefault userDefault = new UserDefault();
        userDefault.setWorkBasketDefaultJurisdiction("J1");
        userDefault.setWorkBasketDefaultCaseType("CT");
        userDefault.setWorkBasketDefaultState("ST");
        List<Jurisdiction> userJurisdictions = newArrayList(j1, j2, unknownJurisdiction);
        userDefault.setJurisdictions(userJurisdictions);
        return userDefault;
    }

    private void initialiseJurisdictions() {
        j1 = new Jurisdiction();
        j1.setId("J1");
        j1.setName("J1Name");
        j1.setDescription("Desc1");
        j2 = new Jurisdiction();
        j2.setId("J2");
        j2.setName("J2Name");
        j2.setDescription("Desc2");
        unknownJurisdiction = new Jurisdiction();
        unknownJurisdiction.setId("J3");
    }
}
