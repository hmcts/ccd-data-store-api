package uk.gov.hmcts.ccd.security.filters;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.ccd.AuthCheckerConfiguration;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.ccd.AuthCheckerConfiguration.CITIZEN_ROLES;

@RunWith(MockitoJUnitRunner.class)
public class V1EndpointsPathParamSecurityFilterTest {

    @Mock
    private SecurityUtils securityUtils;

    private V1EndpointsPathParamSecurityFilter filter;

    private MockHttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @Before
    public void setUp() {
        AuthCheckerConfiguration config = new AuthCheckerConfiguration();
        filter = new V1EndpointsPathParamSecurityFilter(config.userIdExtractor(),
            config.authorizedRolesExtractor(), securityUtils);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    public void shouldReturn200ResponseWhenUserIdAndRoleMatches() throws Exception {
        UserInfo userInfo = UserInfo.builder()
            .uid("user123")
            .roles(Arrays.asList(CITIZEN_ROLES))
            .build();

        request.setRequestURI("/citizens/user123/jurisdictions/AUTOTEST1/case-types");
        given(securityUtils.getUserInfo()).willReturn(userInfo);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void shouldReturn401ResponseWhenUserIdIsNotSame() throws Exception {
        UserInfo userInfo = UserInfo.builder()
            .uid("invalidUser")
            .roles(Lists.newArrayList("caseworker-autotest1"))
            .build();

        request.setRequestURI("/caseworkers/user123/jurisdictions/AUTOTEST1/case-types");
        given(securityUtils.getUserInfo()).willReturn(userInfo);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    public void shouldReturn401ResponseWhenRoleNotMatches() throws Exception {
        UserInfo userInfo = UserInfo.builder()
            .uid("user123")
            .roles(Lists.newArrayList("caseworker-autotest123"))
            .build();

        request.setRequestURI("/caseworkers/user123/jurisdictions/AUTOTEST1/case-types");

        given(securityUtils.getUserInfo()).willReturn(userInfo);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    public void shouldReturn401ResponseWhenCitizenPathRequestNotHavingValidRole() throws Exception {
        UserInfo userInfo = UserInfo.builder()
            .uid("user123")
            .roles(Lists.newArrayList("caseworker-autotest1"))
            .build();

        request.setRequestURI("/citizens/user123/jurisdictions/AUTOTEST1/case-types");

        given(securityUtils.getUserInfo()).willReturn(userInfo);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
    }
}
