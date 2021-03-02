package uk.gov.hmcts.ccd.infrastructure.user;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

@DisplayName("UserAuthorisationFactory")
class UserAuthorisationConfigurationTest {

    private static final String USER_ID = "123";
    private static final UserAuthorisation.AccessLevel ACCESS_LEVEL = UserAuthorisation.AccessLevel.GRANTED;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private Authentication authentication;

    private UserInfo userInfo;

    @Mock
    private CaseAccessService caseAccessService;

    @InjectMocks
    private UserAuthorisationConfiguration factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        userInfo = UserInfo.builder()
            .uid(USER_ID)
            .sub("uid@mail.com")
            .name("aName")
            .roles(Lists.newArrayList("role1", "role2"))
            .build();

        when(securityUtils.getUserInfo()).thenReturn(userInfo);

        when(caseAccessService.getAccessLevel(userInfo)).thenReturn(ACCESS_LEVEL);
    }

    @Test
    @DisplayName("should create user authorisation with ID")
    void shouldCreateAuthorisationForUserID() {
        final UserAuthorisation userAuthorisation = factory.create();

        assertThat(userAuthorisation.getUserId(), equalTo(USER_ID));
    }

    @Test
    @DisplayName("should create user authorisation with access level")
    void shouldCreateAuthorisationWithAccessLevel() {
        final UserAuthorisation userAuthorisation = factory.create();

        assertThat(userAuthorisation.getAccessLevel(), equalTo(ACCESS_LEVEL));
    }

    @Test
    @DisplayName("should create user authorisation with roles")
    void shouldCreateAuthorisationWithRoles() {
        final UserAuthorisation userAuthorisation = factory.create();

        assertThat(userAuthorisation.getRoles(), hasSize(2));
    }

}
