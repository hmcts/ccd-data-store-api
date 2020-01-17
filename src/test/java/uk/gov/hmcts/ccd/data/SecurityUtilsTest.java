package uk.gov.hmcts.ccd.data;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

@DisplayName("SecurityUtils")
class SecurityUtilsTest {

    private static final String SERVICE_JWT = "7gf364fg367f67";
    private static final String USER_ID = "123";
    private static final String USER_JWT = "Bearer 8gf364fg367f67";

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private ServiceAndUserDetails principal;

    @Mock
    private AuthTokenGenerator serviceTokenGenerator;

    @InjectMocks
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        final GrantedAuthority[] authorities = new GrantedAuthority[] { newAuthority("role1"), newAuthority("role2")};

        when(serviceTokenGenerator.generate()).thenReturn(SERVICE_JWT);

        doReturn(Arrays.asList(authorities)).when(principal).getAuthorities();
        doReturn(USER_ID).when(principal).getUsername();
        doReturn(principal).when(authentication).getPrincipal();
        doReturn(USER_JWT).when(principal).getPassword();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("authorizationHeaders")
    void authorizationHeaders() {
        final HttpHeaders headers = securityUtils.authorizationHeaders();

        assertAll(
            () -> assertHeader(headers, "ServiceAuthorization", SERVICE_JWT),
            () -> assertHeader(headers, "user-id", USER_ID),
            () -> assertHeader(headers, "user-roles", "role1,role2")
        );
    }

    @Test
    @DisplayName("Get user token")
    void shouldReturnUserToken() {
        assertThat(securityUtils.getUserToken(), is(USER_JWT));
    }

    private void assertHeader(HttpHeaders headers, String name, String value) {
        assertThat(headers.get(name), hasSize(1));
        assertThat(headers.get(name).get(0), equalTo(value));
    }

    private GrantedAuthority newAuthority(String authority) {
        return (GrantedAuthority) () -> authority;
    }
}
