package uk.gov.hmcts.ccd.security.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.ccd.security.exception.UnauthorizedException;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceAuthFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceAuthFilter.class);

    public static final String AUTHORISATION = "ServiceAuthorization";

    private final List<String> authorisedServices;
    private final AuthTokenValidator authTokenValidator;

    public ServiceAuthFilter(AuthTokenValidator authTokenValidator, List<String> authorisedServices) {
        this.authTokenValidator = authTokenValidator;
        if (authorisedServices.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one service defined");
        }
        this.authorisedServices = authorisedServices.stream().map(String::toLowerCase).collect(Collectors.toList());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            authorise(request);
        } catch (UnauthorizedException ex) {
            LOG.warn("Unsuccessful service authentication", ex);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String authorise(HttpServletRequest request) throws UnauthorizedException {
        String bearerToken = extractBearerToken(request);
        String serviceName;
        try {
            serviceName = authTokenValidator.getServiceName(bearerToken);
        } catch (InvalidTokenException | ServiceException ex) {
            throw new UnauthorizedException(ex.getMessage(), ex.getCause());
        }
        if (!authorisedServices.contains(serviceName)) {
            throw new UnauthorizedException("Unauthorised service access");
        }
        return serviceName;
    }

    private String extractBearerToken(HttpServletRequest request) {
        String token = request.getHeader(AUTHORISATION);
        if (token == null) {
            throw new UnauthorizedException("ServiceAuthorization Token is missing");
        }
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }


}
