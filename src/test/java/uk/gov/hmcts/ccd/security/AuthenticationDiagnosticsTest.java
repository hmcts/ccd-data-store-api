package uk.gov.hmcts.ccd.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationDiagnosticsTest {

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403})
    void retainsPrivateDimensionsAndOriginalDiagnosticWithoutChangingResponse(int status) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(SafeAuthenticationErrors.class);
        Level previousLevel = logger.getLevel();
        var previousContext = MDC.getCopyOfContextMap();
        var events = new ListAppender<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        events.start();
        logger.addAppender(events);
        try {
            logger.setLevel(Level.INFO);
            MDC.clear();
            MDC.put("operationId", "test-operation");
            MDC.put("failureCode", "outer-code");
            MDC.put("httpStatus", "outer-status");
            var handler = new SafeAuthenticationErrors();
            var request = new MockHttpServletRequest();
            var response = new MockHttpServletResponse();
            if (status == 403) {
                handler.handle(request, response, new AccessDeniedException("PRIVATE cause"));
            } else if (status == 400) {
                handler.commence(request, response,
                    new OAuth2AuthenticationException(BearerTokenErrors.invalidRequest("PRIVATE cause")));
            } else {
                handler.commence(request, response, new BadCredentialsException("PRIVATE cause"));
            }
            assertThat(response.getStatus()).isEqualTo(status);
            assertThat(response.getContentAsString()).isEmpty();
            assertThat(events.list).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(status == 500 ? Level.ERROR : Level.WARN);
                assertThat(event.getMDCPropertyMap())
                    .containsEntry("failureCode", status == 403 ? "AUTHORISATION_DENIED" : "AUTHENTICATION_FAILED")
                    .containsEntry("httpStatus", Integer.toString(status))
                    .containsEntry("operationId", "test-operation");
                assertThat(event.getThrowableProxy()).isNotNull();
                assertThat(event.getThrowableProxy().getStackTraceElementProxyArray()).isNotEmpty();
            });
            assertThat(MDC.get("failureCode")).isEqualTo("outer-code");
            assertThat(MDC.get("httpStatus")).isEqualTo("outer-status");
            assertThat(MDC.get("operationId")).isEqualTo("test-operation");
        } finally {
            logger.detachAppender(events);
            events.stop();
            logger.setLevel(previousLevel);
            MDC.clear();
            if (previousContext != null) {
                MDC.setContextMap(previousContext);
            }
        }
    }
}
