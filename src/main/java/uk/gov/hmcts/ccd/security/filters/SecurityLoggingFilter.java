package uk.gov.hmcts.ccd.security.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Used to log information about requests and the invoking user. To be enabled this filter should be registered after
 * successful authentication as part of the Spring Security filter chain.
 */
@Slf4j
public class SecurityLoggingFilter extends OncePerRequestFilter {

    private static final String LOG_MESSAGE_TEMPLATE = "Attempting to serve request %s %s for user with IDAM roles %s";

    private final SecurityUtils securityUtils;
    private final Pattern pathPattern;

    public SecurityLoggingFilter(SecurityUtils securityUtils, String pathRegex) {
        this.securityUtils = securityUtils;
        this.pathPattern = Pattern.compile(pathRegex);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        log.debug(buildLogMessage(request, securityUtils.getUserInfo()));
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !pathPattern.matcher(request.getRequestURI()).matches();
    }


    private String buildLogMessage(HttpServletRequest request, UserInfo userInfo) {
        return String.format(LOG_MESSAGE_TEMPLATE,
            request.getMethod(), request.getRequestURI(), String.join(",", userInfo.getRoles()));
    }
}
