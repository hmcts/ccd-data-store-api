package uk.gov.hmcts.ccd;

import org.springframework.security.core.Authentication;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;

import java.util.Arrays;

import static org.mockito.Mockito.when;

public class MockUtils {

    public static final String ROLE_CASEWORKER_PUBLIC = "caseworker-probate-public";
    public static final String ROLE_CASEWORKER_PRIVATE = "caseworker-probate-private";
    public static final String ROLE_TEST_PUBLIC = "caseworker-test-public";
    public static final String ROLE_CITIZEN = "citizen";

    public static final void setSecurityAuthorities(Authentication authenticationMock, String... authorities) {
        String username = "someusername";
        String token = "Bearer jwtToken";
        String serviceName = "ccd-data";
        Object principal = new ServiceAndUserDetails(username, token, Arrays.asList(authorities), serviceName);
        when(authenticationMock.getPrincipal()).thenReturn(principal);
    }
}
