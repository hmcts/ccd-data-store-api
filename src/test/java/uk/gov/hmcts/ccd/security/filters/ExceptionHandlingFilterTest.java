package uk.gov.hmcts.ccd.security.filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ExceptionHandlingFilterTest {

    private MockHttpServletRequest request;

    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ExceptionHandlingFilter filter;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    private static final String EXCEPTION_MESSAGE = "Exception thrown in the security filter chain";
    private static final String EXCEPTION_LOGGER_TYPE = "uk.gov.hmcts.ccd.security.filters.ExceptionHandlingFilter";

    @Before
    public void setUp() {
        filter = new ExceptionHandlingFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        logger = (Logger) LoggerFactory.getLogger(ExceptionHandlingFilter.class);
        // create and start a ListAppender
        listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        logger.addAppender(listAppender);
    }

    @After
    public void tearDown() {
        listAppender.stop();
        logger.detachAppender(listAppender);
    }

    @Test
    public void shouldReturn502ResponseWhenClientAbortExceptionThrown() throws ServletException, IOException {
        Mockito.doThrow(ClientAbortException.class)
            .when(filterChain)
            .doFilter(Mockito.eq(request), Mockito.eq(response));
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(502);
        assertLogging();
    }

    @Test
    public void shouldReturn500ResponseWhenAnyExceptionExceptClientAbortExceptionThrown()
        throws ServletException, IOException {
        Mockito.doThrow(NullPointerException.class)
            .when(filterChain)
            .doFilter(Mockito.eq(request), Mockito.eq(response));
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(500);
        assertLogging();
    }

    private void assertLogging() {
        List<ILoggingEvent> logsList = listAppender.list;
        ILoggingEvent lastLogEntry = logsList.get(logsList.size() - 1);

        assertThat(lastLogEntry.getLevel()).isEqualTo(Level.ERROR);
        assertThat(lastLogEntry.getLoggerName()).isEqualTo(EXCEPTION_LOGGER_TYPE);
        assertThat(lastLogEntry.getMessage()).contains(EXCEPTION_MESSAGE);
    }
}
