package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

class AuthCheckingConfigurationTest {

    @Mock
    private ApplicationParams applicationParams;

    private AuthCheckerConfiguration authCheckerConfiguration;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        authCheckerConfiguration = new AuthCheckerConfiguration(applicationParams);
    }

    @Test
    @DisplayName("should return authorised roles for caseworker endpoint with jurisdiction")
    void validCaseworkerRoleWithJurisdiction() {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("http://caseworkers/123/jurisdictions/test/cases");
        final Collection<String> roles = authCheckerConfiguration.authorizedRolesExtractor().apply(request);

        assertAll(
            () -> assertThat(roles, hasSize(1)),
            () -> assertThat(roles, hasItem("caseworker-test"))
        );
    }

    @Test
    @DisplayName("should return authorised roles for citizen endpoint")
    void shouldAuthoriseRolesForCitizenEndpoint() {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("http://citizens/123/jurisdictions/test/cases");

        when(applicationParams.getCcdAccessControlCitizenRoles()).thenReturn(Arrays.asList("citizen", "letter-holder"));
        final Collection<String> response = authCheckerConfiguration.authorizedRolesExtractor().apply(request);

        assertAll(
            () -> assertThat(response, hasSize(2)),
            () -> assertThat(response, hasItems(authCheckerConfiguration.getCitizenRoles()))
        );
    }

    @Test
    @DisplayName("should extract caseworker ID")
    void validCaseworkerUserId() {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("http://caseworkers/123/jurisdictions/test/cases");
        final Optional<String> id = authCheckerConfiguration.userIdExtractor().apply(request);


        assertAll(
            () -> assertThat(id.isPresent(), is(Boolean.TRUE)),
            () -> assertThat(id.get(), equalTo("123"))
        );
    }

    @Test
    @DisplayName("should extract citizen ID")
    void validCitizenUserId() {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("http://citizens/345/jurisdictions/test/cases");
        final Optional<String> id = authCheckerConfiguration.userIdExtractor().apply(request);

        assertAll(
            () -> assertThat(id.isPresent(), is(Boolean.TRUE)),
            () -> assertThat(id.get(), equalTo("345"))
        );
    }

    @Test
    @DisplayName("should handle missing user ID")
    void noUserId() {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("http://jurisdictions/test/cases");
        final Optional<String> id = authCheckerConfiguration.userIdExtractor().apply(request);
        assertThat(id.isPresent(), is(Boolean.FALSE));
    }

    @Test
    @DisplayName("should handle missing roles")
    void noRoles() {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("http://jurisdictions/test/cases");
        final Collection<String> roles = authCheckerConfiguration.authorizedRolesExtractor().apply(request);

        assertThat(roles, hasSize(0));
    }
}

