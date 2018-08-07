package uk.gov.hmcts.ccd.data.user;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

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

    private Jurisdiction j1;
    private Jurisdiction j2;
    private Jurisdiction unknownJurisdiction;
    private UserService userService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        userService = new UserService(userRepoMock, caseDefinitionRepoMock, jurisdictionMapperMock);
        when(mockIDAMProps.getEmail()).thenReturn("email");
        initialiseJurisdictions();
    }

    @Test
    public void testReturnsUserProfileDiscardingUnknownJurisdictions() {

        when(userRepoMock.getUserDetails()).thenReturn(mockIDAMProps);
        UserDefault userDefaultMock = aUserDefault();
        when(userRepoMock.getUserDefaultSettings("email")).thenReturn(userDefaultMock);
        List<Jurisdiction> jurisdictionsDef = newArrayList(j1, j2);
        when(caseDefinitionRepoMock.getJurisdictions(anyList())).thenReturn(jurisdictionsDef);
        when(jurisdictionMapperMock.toResponse(j1)).thenReturn(jdp1);
        when(jurisdictionMapperMock.toResponse(j2)).thenReturn(jdp2);

        UserProfile userProfile = userService.getUserProfile();

        assertThat(userProfile.getUser().getIdamProperties(), is(mockIDAMProps));
        assertThat(userProfile.getJurisdictions(), equalTo(new JurisdictionDisplayProperties[] {jdp1, jdp2}));
        WorkbasketDefault workbasketDefault = userProfile.getDefaultSettings().getWorkbasketDefault();
        assertThat(workbasketDefault.getJurisdictionId(), is("J1"));
        assertThat(workbasketDefault.getCaseTypeId(), is("CT"));
        assertThat(workbasketDefault.getStateId(), is("ST"));
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
