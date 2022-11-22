package uk.gov.hmcts.ccd.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.firewall.RequestRejectedException;
import uk.gov.hmcts.ccd.appinsights.AppInsights;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataStoreHttpStatusRequestRejectedHandlerTest {

    private static final String EXCEPTION_MESSAGE = "Request Rejected Exception Test Message";

    @Mock
    private AppInsights appInsights;

    private MockHttpServletRequest mockHttpServletRequest;
    private MockHttpServletResponse mockHttpServletResponse;
    private RequestRejectedException requestRejectedException;

    private AutoCloseable autoCloseable;

    private Logger handlerLogger;
    private ListAppender<ILoggingEvent> handlerLoggerListAppender;

    @Before
    public void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);

        mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletResponse = new MockHttpServletResponse();

        requestRejectedException = mock(RequestRejectedException.class);
        when(requestRejectedException.getMessage()).thenReturn(EXCEPTION_MESSAGE);

        // Get the logger for the DataStoreHttpStatusRequestRejectedHandler class
        handlerLogger = (Logger) LoggerFactory.getLogger(DataStoreHttpStatusRequestRejectedHandler.class);

        // Create and start a list appender then add it to the logger
        handlerLoggerListAppender = new ListAppender<>();
        handlerLoggerListAppender.start();
        handlerLogger.addAppender(handlerLoggerListAppender);
    }

    @After
    public void tearDown() throws Exception {
        autoCloseable.close();
        handlerLogger.detachAndStopAllAppenders();
    }

    private void checkDataStoreHttpStatusRequestRejectedHandler(DataStoreHttpStatusRequestRejectedHandler handler,
                                                                int expectedHttpError) {
        try {
            handler.handle(mockHttpServletRequest, mockHttpServletResponse, requestRejectedException);
        } catch (final IOException e) {
            fail("Unexpected IOException raised: " + e.getMessage());
        }

        // Confirm that exception was passed to application insights
        verify(appInsights).trackException(requestRejectedException);

        // Confirm that response contains expected http error and exception message
        assertThat(mockHttpServletResponse.getStatus(), is(equalTo(expectedHttpError)));
        assertThat(mockHttpServletResponse.getErrorMessage(), is(equalTo(EXCEPTION_MESSAGE)));

        // Confirm that exception message was written to log and at the expected logging level
        List<ILoggingEvent> loggerList = handlerLoggerListAppender.list;
        ILoggingEvent lastLogEntry = loggerList.get(loggerList.size() - 1);
        assertThat(lastLogEntry.getLevel(), is(equalTo(Level.WARN)));
        assertThat(lastLogEntry.getMessage(), containsString(EXCEPTION_MESSAGE));
    }

    /**
     * Test behaviour of DataStoreHttpStatusRequestRejectedHandler when it is initialised with the
     * default http error code.
     */
    @Test
    public void handlerCreatedWithDefaultHttpError() {
        DataStoreHttpStatusRequestRejectedHandler handler = new DataStoreHttpStatusRequestRejectedHandler(appInsights);
        checkDataStoreHttpStatusRequestRejectedHandler(handler, HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Test behaviour of DataStoreHttpStatusRequestRejectedHandler when it is initialised with a
     * specific http error code.
     */
    @Test
    public void handlerCreatedWithSpecificHttpError() {
        DataStoreHttpStatusRequestRejectedHandler handler =
            new DataStoreHttpStatusRequestRejectedHandler(appInsights, HttpServletResponse.SC_BAD_REQUEST);
        checkDataStoreHttpStatusRequestRejectedHandler(handler, HttpServletResponse.SC_BAD_REQUEST);
    }
}
