package uk.gov.hmcts.ccd.security;

import uk.gov.hmcts.ccd.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Keeps authentication diagnostics in server logs, not in the Bearer challenge. */
@Slf4j
public class SafeAuthenticationErrors implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String challenge = "Bearer";
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            if (oauthException.getError() instanceof BearerTokenError bearerError) {
                status = bearerError.getHttpStatus();
            }
            String error = oauthException.getError().getErrorCode();
            if (java.util.Set.of(BearerTokenErrorCodes.INVALID_REQUEST, BearerTokenErrorCodes.INVALID_TOKEN,
                BearerTokenErrorCodes.INSUFFICIENT_SCOPE).contains(error)) {
                challenge += " error=\"" + error + "\"";
            }
        }
        ErrorCode code = ErrorCode.fromStatus(status.value());
        log.warn("Authentication failed: code={} status={}", code, status.value(), exception);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge);
        response.setStatus(status.value());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) {
        log.warn("Access denied: code={} status=403", ErrorCode.ACCESS_DENIED, exception);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
            request.getUserPrincipal() instanceof AbstractOAuth2TokenAuthenticationToken<?>
                ? "Bearer error=\"insufficient_scope\"" : "Bearer");
        response.setStatus(HttpStatus.FORBIDDEN.value());
    }
}
