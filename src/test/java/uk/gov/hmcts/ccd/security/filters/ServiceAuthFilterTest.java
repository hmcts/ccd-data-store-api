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
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.ccd.security.filters.ServiceAuthFilter.AUTHORISATION;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAuthFilterTest {

    private static String TOKEN = "Bearer s2s token";

    @Mock
    private AuthTokenValidator authTokenValidator;

    private ServiceAuthFilter serviceAuthFilter;

    private MockHttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @Before
    public void setUp() {
        serviceAuthFilter = new ServiceAuthFilter(authTokenValidator, Lists.newArrayList("ccd_gw"));
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();

    }

    @Test
    public void shouldReturn401ResponseWhenTokenHearNotPresent() throws Exception {

        serviceAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    public void shouldReturn401ResponseWhenTokenIsInValid() throws Exception {

        request.addHeader(AUTHORISATION, TOKEN);
        given(authTokenValidator.getServiceName(TOKEN))
            .willThrow(new InvalidTokenException("invalid", new RuntimeException()));

        serviceAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    public void shouldProceedInTheFilterChainAfterToneValidation() throws Exception {

        request.addHeader(AUTHORISATION, TOKEN);
        given(authTokenValidator.getServiceName(TOKEN)).willReturn("ccd_gw");

        serviceAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
