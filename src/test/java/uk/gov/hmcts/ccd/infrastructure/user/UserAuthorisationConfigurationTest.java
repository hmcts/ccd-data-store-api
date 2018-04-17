package uk.gov.hmcts.ccd.infrastructure.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@DisplayName("UserAuthorisationFactory")
class UserAuthorisationConfigurationTest {

    private static final String USER_ID = "123";
    private static final UserAuthorisation.AccessLevel ACCESS_LEVEL = UserAuthorisation.AccessLevel.GRANTED;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private ServiceAndUserDetails serviceAndUser;

    @Mock
    private CaseAccessService caseAccessService;

    @InjectMocks
    private UserAuthorisationConfiguration factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(serviceAndUser);

        when(serviceAndUser.getUsername()).thenReturn(USER_ID);
        when(caseAccessService.getAccessLevel(serviceAndUser)).thenReturn(ACCESS_LEVEL);
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

}
