package uk.gov.hmcts.ccd.domain.service.aggregated;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.user.UserService;
import uk.gov.hmcts.ccd.domain.model.aggregated.DefaultSettings;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.User;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;

public class DefaultGetUserProfileOperationTest {

    @Mock
    private UserService userService;

    private DefaultGetUserProfileOperation defaultGetUserProfileOperation;

    private final UserProfile userProfile = new UserProfile();
    private final User user = new User();
    private final JurisdictionDisplayProperties test1JurisdictionDisplayProperties =
        new JurisdictionDisplayProperties();
    private final JurisdictionDisplayProperties test2JurisdictionDisplayProperties =
        new JurisdictionDisplayProperties();
    private final String test1Channel = "test1Channel";
    private final String test2Channel = "test2Channel";
    private final DefaultSettings testDefaultSettings = new DefaultSettings();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        defaultGetUserProfileOperation = new DefaultGetUserProfileOperation(userService);

        userProfile.setUser(user);
        userProfile.setJurisdictions(new JurisdictionDisplayProperties[] {test1JurisdictionDisplayProperties,
            test2JurisdictionDisplayProperties});
        userProfile.setChannels(new String[] {test1Channel, test2Channel});
        userProfile.setDefaultSettings(testDefaultSettings);
        doReturn(userProfile).when(userService).getUserProfile();
    }

    @Test
    void shouldReturnUserProfile() {
        UserProfile userProfile = defaultGetUserProfileOperation.execute(CAN_READ);

        assertAll(
            () -> assertThat(userProfile.getUser(), is(equalTo(user))),
            () -> assertThat(userProfile.getDefaultSettings(), is(equalTo(testDefaultSettings))),
            () -> assertThat(userProfile.getChannels(), arrayContainingInAnyOrder(test1Channel, test2Channel)),
            () -> assertThat(userProfile.getJurisdictions(),
                arrayContainingInAnyOrder(test1JurisdictionDisplayProperties, test2JurisdictionDisplayProperties))
        );

    }
}
