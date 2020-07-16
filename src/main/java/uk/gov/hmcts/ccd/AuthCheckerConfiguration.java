package uk.gov.hmcts.ccd;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Configuration
public class AuthCheckerConfiguration {

    @Autowired
    private ApplicationParams applicationParams;

    public String[] getCitizenRoles() {
        return applicationParams.getCcdAccessControlCitizenRoles().stream().toArray(String[]::new);
    }

    @Autowired
    public AuthCheckerConfiguration(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Bean
    public Function<HttpServletRequest, Optional<String>> userIdExtractor() {
        final Pattern caseworkerPattern = Pattern.compile("/caseworkers/([^/]+)/.+$");
        final Pattern citizenPattern = Pattern.compile("/citizens/([^/]+)/.+$");
        return request -> {
            final Matcher caseworkerMatcher = caseworkerPattern.matcher(request.getRequestURI());
            final Matcher citizenMatcher = citizenPattern.matcher(request.getRequestURI());
            if (caseworkerMatcher.find()) {
                return Optional.ofNullable(caseworkerMatcher.group(1));
            } else if (citizenMatcher.find()) {
                return Optional.ofNullable(citizenMatcher.group(1));
            }
            return Optional.empty();
        };
    }

    @Bean
    public Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor() {
        return request -> {
            final Collection<String> roles = Lists.newArrayList();

            final Pattern caseWorkerPattern = Pattern.compile("/caseworkers/([^/]+)/.+$");
            final Matcher caseworkerMatcher = caseWorkerPattern.matcher(request.getRequestURI());
            final Pattern citizenWorkerPattern = Pattern.compile("/citizens/([^/]+)/.+$");
            final Matcher citizenMatcher = citizenWorkerPattern.matcher(request.getRequestURI());

            if (caseworkerMatcher.find()) {
                final StringBuilder role = new StringBuilder("caseworker");
                final Pattern jurisdictionPattern = Pattern.compile("/jurisdictions?/([^/]+)/.+$");
                final Matcher jurisdictionMatcher = jurisdictionPattern.matcher(request.getRequestURI());
                if (jurisdictionMatcher.find()) {
                    role.append("-")
                            .append(jurisdictionMatcher.group(1).toLowerCase());
                }
                roles.add(role.toString());
            } else if (citizenMatcher.find()) {
                roles.addAll(this.applicationParams.getCcdAccessControlCitizenRoles());
            }

            return roles;
        };
    }
}
