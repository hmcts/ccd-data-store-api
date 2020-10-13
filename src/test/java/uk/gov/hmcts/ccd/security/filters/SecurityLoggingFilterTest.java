package uk.gov.hmcts.ccd.security.filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SecurityLoggingFilterTest {

    private static final List<String> ROLES_LIST = Arrays.asList(
        "ccd-import",
        "caseworker-autotest1",
        "caseworker"
    );
    private static final String REQUEST_URI = "/url/path?with=param";

    private SecurityLoggingFilter filter;

    @Mock
    private SecurityUtils securityUtils;

    private MockHttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    private ListAppender<ILoggingEvent> filterLoggerCapture;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();

        Logger filterLogger = (Logger) LoggerFactory.getLogger(SecurityLoggingFilter.class);
        filterLogger.setLevel(Level.DEBUG);
        filterLoggerCapture = new ListAppender<>();
        filterLoggerCapture.start();
        filterLogger.addAppender(filterLoggerCapture);
    }

    @Test
    void shouldLogRolesOfInvoker() throws Exception {
        filter = new SecurityLoggingFilter(securityUtils, "/first-url.*|/url.*");

        UserInfo userInfo = UserInfo.builder()
            .uid("user123")
            .roles(ROLES_LIST)
            .build();

        request.setMethod("POST");
        request.setRequestURI(REQUEST_URI);
        given(securityUtils.getUserInfo()).willReturn(userInfo);

        filter.doFilterInternal(request, response, filterChain);

        String expectedMessage = "Attempting to serve request POST /url/path?with=param for user with IDAM roles "
            + "ccd-import,caseworker-autotest1,caseworker";
        List<ILoggingEvent> loggingEvents = filterLoggerCapture.list;
        assertAll(
            () -> assertEquals(1, loggingEvents.size()),
            () -> assertEquals(expectedMessage, loggingEvents.get(0).getFormattedMessage()),
            () -> assertEquals(Level.DEBUG, loggingEvents.get(0).getLevel())
        );
    }

    @Test
    void shouldNotFilterWhenUrlDoesNotMatchRegex() throws Exception {
        filter = new SecurityLoggingFilter(securityUtils, "/no-match.*");
        request.setRequestURI(REQUEST_URI);

        boolean result = filter.shouldNotFilter(request);

        assertTrue(result);
    }

    @Test
    void shouldFilterWhenUrlMatchesRegex() throws Exception {
        filter = new SecurityLoggingFilter(securityUtils, "/first-url.*|/url.*");
        request.setRequestURI(REQUEST_URI);

        boolean result = filter.shouldNotFilter(request);

        assertFalse(result);
    }
}
