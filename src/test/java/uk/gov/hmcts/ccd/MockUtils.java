package uk.gov.hmcts.ccd;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

public class MockUtils {

    private MockUtils() {}

    public static final String ROLE_CASEWORKER_PUBLIC = "caseworker-probate-public";
    public static final String ROLE_CASEWORKER_PRIVATE = "caseworker-probate-private";
    public static final String ROLE_TEST_PUBLIC = "caseworker-test-public";
    public static final String ROLE_CITIZEN = "citizen";
    public static final String CASE_ROLE_CAN_CREATE = "[CAN_CREATE]";
    public static final String CASE_ROLE_CAN_READ = "[CAN_READ]";
    public static final String CASE_ROLE_CAN_UPDATE = "[CAN_UPDATE]";
    public static final String CASE_ROLE_CAN_DELETE = "[CAN_DELETE]";

    public static final void setSecurityAuthorities(Authentication authenticationMock, String... authorities) {
        setSecurityAuthorities("aJwtToken", authenticationMock, authorities);
    }

    public static final void setSecurityAuthorities(String jwtToken, Authentication authenticationMock, String... authorities) {

        Jwt jwt =   Jwt.withTokenValue(jwtToken)
            .claim("aClaim", "aClaim")
            .header("aHeader", "aHeader")
            .build();
        when(authenticationMock.getPrincipal()).thenReturn(jwt);

        Collection<? extends GrantedAuthority> authorityCollection = Stream.of(authorities)
            .map(a -> new SimpleGrantedAuthority(a))
            .collect(Collectors.toCollection(ArrayList::new));

        when(authenticationMock.getAuthorities()).thenAnswer(invocationOnMock -> authorityCollection);

    }
}
