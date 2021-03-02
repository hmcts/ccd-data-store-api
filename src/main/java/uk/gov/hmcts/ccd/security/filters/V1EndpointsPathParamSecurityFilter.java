package uk.gov.hmcts.ccd.security.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.security.exception.UnauthorizedException;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

public class V1EndpointsPathParamSecurityFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(V1EndpointsPathParamSecurityFilter.class);

    private final Function<HttpServletRequest, Optional<String>> userIdExtractor;
    private final Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor;
    private final SecurityUtils securityUtils;

    public V1EndpointsPathParamSecurityFilter(Function<HttpServletRequest, Optional<String>> userIdExtractor,
                                              Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor,
                                              SecurityUtils securityUtils) {
        this.userIdExtractor = userIdExtractor;
        this.authorizedRolesExtractor = authorizedRolesExtractor;
        this.securityUtils = securityUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        Collection<String> authorizedRoles = authorizedRolesExtractor.apply(request);
        Optional<String> userIdOptional = userIdExtractor.apply(request);

        if (securityUtils.isAuthenticated() && (!authorizedRoles.isEmpty() || userIdOptional.isPresent())) {
            try {
                verifyRoleAndUserId(authorizedRoles, userIdOptional);
            } catch (UnauthorizedException ex) {
                LOG.warn("Unauthorised roles or userId in the request path", ex);
                response.sendError(HttpStatus.FORBIDDEN.value(), "Access Denied");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void verifyRoleAndUserId(Collection<String> authorizedRoles, Optional<String> userIdOptional) {
        UserInfo userInfo = securityUtils.getUserInfo();
        if (!authorizedRoles.isEmpty() && Collections.disjoint(authorizedRoles, userInfo.getRoles())) {
            throw new UnauthorizedException("Unauthorised role in the path");
        }
        userIdOptional.ifPresent(resourceUserId -> {
            if (!resourceUserId.equalsIgnoreCase(userInfo.getUid())) {
                throw new UnauthorizedException("Unauthorised userId in the path");
            }
        });
    }

}
