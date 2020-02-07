package uk.gov.hmcts.ccd.v2.internal.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.UserProfileBuilder.newUserProfile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.aggregated.DefaultSettings;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.User;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;

import java.util.Optional;

@DisplayName("UserProfileResource")
class UIUserProfileResourceTest {
    private static final String LINK_SELF = String.format("/internal/profile");

    @Mock
    private User user;

    @Mock
    private DefaultSettings defaultSettings;

    @Mock
    private JurisdictionDisplayProperties jurisdictionDisplayProperties1;

    @Mock
    private JurisdictionDisplayProperties jurisdictionDisplayProperties2;

    private JurisdictionDisplayProperties[] jurisdictionDisplayProperties;

    private String channel1 = "channel1";

    private String channel2 = "channel2";

    private String[] channels;

    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        channels = new String[]{channel1, channel2};
        jurisdictionDisplayProperties = new JurisdictionDisplayProperties[]{jurisdictionDisplayProperties1, jurisdictionDisplayProperties2};
        userProfile = newUserProfile()
            .withUser(user)
            .withChannels(channels)
            .withDefaultSettings(defaultSettings)
            .withJurisdictionDisplayProperties(jurisdictionDisplayProperties)
            .build();
    }

    @Test
    @DisplayName("should copy user profile")
    void shouldCopyUserProfile() {
        final UIUserProfileResource resource = new UIUserProfileResource(userProfile);

        assertAll(
            () -> assertThat(resource.getUserProfile().getUser(), sameInstance(user)),
            () -> assertThat(resource.getUserProfile().getJurisdictions(), sameInstance(jurisdictionDisplayProperties)),
            () -> assertThat(resource.getUserProfile().getDefaultSettings(), sameInstance(defaultSettings)),
            () -> assertThat(resource.getUserProfile().getChannels(), sameInstance(channels))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final UIUserProfileResource resource = new UIUserProfileResource(userProfile);

        Optional<Link> self = resource.getLink("self");

        assertThat(self.get().getHref(), equalTo(LINK_SELF));
    }

}
