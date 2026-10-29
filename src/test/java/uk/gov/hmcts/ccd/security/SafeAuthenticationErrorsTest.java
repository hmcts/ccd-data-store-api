package uk.gov.hmcts.ccd.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class SafeAuthenticationErrorsTest {

    @Test
    void omitsAuthenticationDiagnosticsFromHeadersAndBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new SafeAuthenticationErrors().commence(new MockHttpServletRequest(), response,
            new BadCredentialsException("Internal decoder at http://internal-service/jwks"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void preservesInvalidBearerRequestStatusWithoutDiagnostics() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(SafeAuthenticationErrors.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            logger.setLevel(Level.INFO);
            MockHttpServletResponse response = new MockHttpServletResponse();
            new SafeAuthenticationErrors().commence(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException(BearerTokenErrors.invalidRequest("Synthetic bearer diagnostic")));
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer error=\"invalid_request\"");
            assertThat(response.getContentAsString()).isEmpty();
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage())
                .isEqualTo("Authentication failed: code=INVALID_REQUEST status=400");
            assertThat(appender.list.get(0).getThrowableProxy()).isNotNull();
        } finally {
            logger.setLevel(previousLevel);
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void preservesForbiddenWithoutDiagnostics() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new SafeAuthenticationErrors().handle(new MockHttpServletRequest(), response,
            new AccessDeniedException("Internal access rule details"));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void retainsOnlyAllowlistedBearerProtocolCodes() {
        for (var error : java.util.List.of(
            BearerTokenErrors.invalidToken("private diagnostic"),
            BearerTokenErrors.insufficientScope("private diagnostic", "private-scope"),
            new org.springframework.security.oauth2.core.OAuth2Error("private-error", "private diagnostic",
                "http://private-service"))) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            new SafeAuthenticationErrors().commence(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException(error));
            assertThat(response.getStatus()).isEqualTo(error.getErrorCode().equals("insufficient_scope") ? 403 : 401);
            assertThat(response.getHeader("WWW-Authenticate")).isEqualTo(
                error.getErrorCode().equals("private-error") ? "Bearer" : "Bearer error=\""
                    + error.getErrorCode() + "\"");
        }
    }

    @Test
    void authenticatedAccessDenialRetainsInsufficientScope() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(new org.springframework.security.oauth2.server.resource.authentication
            .JwtAuthenticationToken(org.springframework.security.oauth2.jwt.Jwt.withTokenValue("private-token")
                .header("alg", "none").claim("sub", "private-user-id").build()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        new SafeAuthenticationErrors().handle(request, response, new AccessDeniedException("private-rule"));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer error=\"insufficient_scope\"");
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void logsSafeAuthenticationCodesWithDiagnosticsOnlyAtDebug() {
        Logger logger = (Logger) LoggerFactory.getLogger(SafeAuthenticationErrors.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        SafeAuthenticationErrors handler = new SafeAuthenticationErrors();
        try {
            logger.setLevel(Level.INFO);
            handler.commence(new MockHttpServletRequest(), new MockHttpServletResponse(),
                new BadCredentialsException("Synthetic authentication detail"));
            handler.handle(new MockHttpServletRequest(), new MockHttpServletResponse(),
                new AccessDeniedException("Synthetic access detail"));
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
                "Authentication failed: code=AUTHENTICATION_REQUIRED status=401",
                "Access denied: code=ACCESS_DENIED status=403");
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getThrowableProxy()).isNotNull();
            });
            appender.list.clear();
            logger.setLevel(Level.DEBUG);
            handler.commence(new MockHttpServletRequest(), new MockHttpServletResponse(),
                new BadCredentialsException("Synthetic authentication detail"));
            handler.handle(new MockHttpServletRequest(), new MockHttpServletResponse(),
                new AccessDeniedException("Synthetic access detail"));
            assertThat(appender.list).hasSize(2);
            assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.WARN);
            assertThat(appender.list.get(0).getThrowableProxy().getMessage())
                .isEqualTo("Synthetic authentication detail");
            assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.WARN);
            assertThat(appender.list.get(1).getThrowableProxy().getMessage()).isEqualTo("Synthetic access detail");
        } finally {
            logger.setLevel(previousLevel);
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
