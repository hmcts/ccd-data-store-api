package uk.gov.hmcts.ccd.data;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

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
    private IdamRepository idamRepository;

    @Mock
    private AuthTokenGenerator serviceTokenGenerator;

    @InjectMocks
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        final GrantedAuthority[] authorities = new GrantedAuthority[] { newAuthority("role1"), newAuthority("role2")};

        when(serviceTokenGenerator.generate()).thenReturn(SERVICE_JWT);

        Jwt jwt =   Jwt.withTokenValue(USER_JWT)
            .claim("aClaim", "aClaim")
            .claim("aud", Lists.newArrayList("ccd_gateway"))
            .header("aHeader", "aHeader")
            .build();
        Collection<? extends GrantedAuthority> authorityCollection = Stream.of("role1", "role2")
            .map(a -> new SimpleGrantedAuthority(a))
            .collect(Collectors.toCollection(ArrayList::new));

        doReturn(jwt).when(authentication).getPrincipal();
        doReturn(authentication).when(securityContext).getAuthentication();
        when(authentication.getAuthorities()).thenAnswer(invocationOnMock -> authorityCollection);
        SecurityContextHolder.setContext(securityContext);


        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID)
            .sub("emailId@a.com")
            .build();
        doReturn(userInfo).when(idamRepository).getUserInfo(USER_JWT);
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
    
    @Test
    @DisplayName("Get service name")
    void shouldGetServiceName() {
        assertThat(securityUtils.getServiceName(), is("ccd_gateway"));
    }

    @Test
    @DisplayName("Get service name from token supplied with bearer")
    void getServiceNameFromS2SToken_shouldReturnNameFromTokenWithBearer() {
        // ARRANGE
        String serviceName = "my-service";
        String s2STokenWithBearer = "Bearer " + MockUtils.generateDummyS2SToken(serviceName);

        // ACT
        String result = securityUtils.getServiceNameFromS2SToken(s2STokenWithBearer);

        // ASSERT
        assertThat(result, is(serviceName));
    }

    @Test
    @DisplayName("Get service name from token supplied without bearer")
    void getServiceNameFromS2SToken_shouldReturnNameFromTokenWithoutBearer() {
        // ARRANGE
        String serviceName = "my-service";
        String s2SToken = MockUtils.generateDummyS2SToken(serviceName);

        // ACT
        String result = securityUtils.getServiceNameFromS2SToken(s2SToken);

        // ASSERT
        assertThat(result, is(serviceName));
    }

    private void assertHeader(HttpHeaders headers, String name, String value) {
        assertThat(headers.get(name), hasSize(1));
        assertThat(headers.get(name).get(0), equalTo(value));
    }

    private GrantedAuthority newAuthority(String authority) {
        return (GrantedAuthority) () -> authority;
    }
}
