package uk.gov.hmcts.ccd.security.filters;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(value = "role.logging.enabled", havingValue = "true")
@Slf4j
public class RoleLoggingFilter extends OncePerRequestFilter {

    private static final String LOG_MESSAGE_TEMPLATE = "[ROLE LOG] Attempting to serve request %s %s for user with IDAM roles %s";

    private final AppInsights appInsights;
    private final SecurityUtils securityUtils;
    private final Pattern pathPattern;

    @Autowired
    public RoleLoggingFilter(AppInsights appInsights, SecurityUtils securityUtils, @Value("${role.logging.path.regex}") String pathRegex) {
        this.appInsights = appInsights;
        this.securityUtils = securityUtils;
        this.pathPattern = Pattern.compile(pathRegex);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String message = buildLogMessage(request, securityUtils.getUserInfo());
        log.debug(message);
        appInsights.trackTrace(message, SeverityLevel.Verbose);

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
