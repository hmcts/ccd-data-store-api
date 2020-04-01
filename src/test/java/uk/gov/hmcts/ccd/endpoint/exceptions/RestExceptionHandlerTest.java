package uk.gov.hmcts.ccd.endpoint.exceptions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Lists;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.domain.model.common.HttpError;
import uk.gov.hmcts.ccd.domain.model.std.CaseFieldValidationError;
import uk.gov.hmcts.ccd.domain.model.std.CaseValidationError;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.ui.UserProfileEndpoint;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext  // required for Jenkins agent
@AutoConfigureWireMock(port = 0)
public class RestExceptionHandlerTest {

    // url to trigger chosen test controller
    private static final String TEST_URL = "/caseworkers/123/profile";

    // service used by chosen test controller which we will use to throw exceptions
    @Mock
    private GetUserProfileOperation mockService;

    private MockMvc mockMvc;

    @Mock
    private AppInsights appInsights;

    @Mock
    private ObjectMapperService objectMapperService;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    private final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    @Captor
    private ArgumentCaptor<Map<String, String>> customPropertiesCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        logger = (Logger) LoggerFactory.getLogger(RestExceptionHandler.class);

        // create and start a ListAppender
        listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        logger.addAppender(listAppender);

        // only create classUnderTest after logger configuration
        RestExceptionHandler classUnderTest = new RestExceptionHandler(appInsights, objectMapperService);

        // any controller will do as these tests will force it to error
        final UserProfileEndpoint controller = new UserProfileEndpoint(mockService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(classUnderTest)
            .build();
    }

    @After
    public void tearDown() {
        listAppender.stop();
        logger.detachAppender(listAppender);
    }

    @Test
    public void handleException_shouldReturnHttpErrorResponse() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique generic runtime exception message 1";
        // any runtime exception (that is not an ApiException)
        ArrayIndexOutOfBoundsException expectedException = new ArrayIndexOutOfBoundsException(myUniqueExceptionMessage);

        setupMockServiceToThrowException(expectedException);

        // ACT
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        assertHttpErrorResponse(result, expectedException);
    }

    @Test
    public void handleException_shouldLogExceptionAsError() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique generic runtime exception message 2";
        // any runtime exception (that is not an ApiException)
        ArrayIndexOutOfBoundsException expectedException = new ArrayIndexOutOfBoundsException(myUniqueExceptionMessage);

        setupMockServiceToThrowException(expectedException);

        // ACT
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        List<ILoggingEvent> logsList = listAppender.list;
        ILoggingEvent lastLogEntry = logsList.get(logsList.size() - 1);
        assertThat(lastLogEntry.getLevel(), is(equalTo(Level.ERROR)));
        assertThat(lastLogEntry.getMessage(), containsString(myUniqueExceptionMessage));
    }

    @Test
    public void handleException_shouldTrackExceptionToAppInsights() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique generic exception message 3";
        ApiException expectedException = new ApiException(myUniqueExceptionMessage);

        setupMockServiceToThrowException(expectedException);

        // ACT
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        verify(appInsights, times(1)).trackException(expectedException);
    }

    @Test
    public void handleApiException_shouldReturnHttpErrorResponse() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique API exception message 1";
        ApiException expectedException = new ApiException(myUniqueExceptionMessage);

        setupMockServiceToThrowException(expectedException);

        // ACT
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        assertHttpErrorResponse(result, expectedException);
    }

    @Test
    public void handleApiException_shouldReturnHttpErrorResponse_withDetails() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique API exception message 1.1";
        ApiException expectedException = new ApiException(myUniqueExceptionMessage);
        expectedException.withDetails("test details");

        setupMockServiceToThrowException(expectedException);

        // ACT
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        assertHttpErrorResponse(result, expectedException);
        assertExtraApiExceptionResponseProperties(result, expectedException);
        result.andExpect(jsonPath("$.details").exists());
    }

    @Test
    public void handleApiException_shouldReturnHttpErrorResponse_withCallbackErrors() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique API exception message 1.2";
        ApiException expectedException = new ApiException(myUniqueExceptionMessage);
        expectedException.withErrors(Lists.newArrayList("Error 1", "Error 2"));

        setupMockServiceToThrowException(expectedException);

        // ACT
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        assertHttpErrorResponse(result, expectedException);
        assertExtraApiExceptionResponseProperties(result, expectedException);
        result.andExpect(jsonPath("$.callbackErrors[*]", hasSize(2)));
    }

    @Test
    public void handleApiException_shouldReturnHttpErrorResponse_withCallbackWarnings() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique API exception message 1.3";
        ApiException expectedException = new ApiException(myUniqueExceptionMessage);
        expectedException.withWarnings(Lists.newArrayList("Warning 1", "Warning 2"));

        setupMockServiceToThrowException(expectedException);

        // ACT
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        assertHttpErrorResponse(result, expectedException);
        assertExtraApiExceptionResponseProperties(result, expectedException);
        result.andExpect(jsonPath("$.callbackWarnings[*]", hasSize(2)));
    }

    @Test
    public void handleApiException_shouldLogExceptionAsError() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique API exception message 2";
        ApiException expectedException = new ApiException(myUniqueExceptionMessage);

        setupMockServiceToThrowException(expectedException);

        // ACT
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        List<ILoggingEvent> logsList = listAppender.list;
        ILoggingEvent lastLogEntry = logsList.get(logsList.size() - 1);
        assertThat(lastLogEntry.getLevel(), is(equalTo(Level.ERROR)));
        assertThat(lastLogEntry.getMessage(), containsString(myUniqueExceptionMessage));
    }

    @Test
    public void handleApiException_shouldTrackExceptionToAppInsights() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique API exception message 3";
        ApiException expectedException = new ApiException(myUniqueExceptionMessage);

        setupMockServiceToThrowException(expectedException);

        // ACT
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        verify(appInsights, times(1)).trackException(expectedException);
    }

    @Test
    public void handleSearchRequestException_shouldReturnHttpErrorResponse() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique BadSearchRequest exception message 1";
        BadSearchRequest expectedException = new BadSearchRequest(myUniqueExceptionMessage);

        setupMockServiceToThrowException(expectedException);

        // ACT
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        assertHttpErrorResponse(result, expectedException);
    }

    @Test
    public void handleSearchRequestException_shouldLogExceptionAsWarning() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique BadSearchRequest exception message 2";
        BadSearchRequest expectedException = new BadSearchRequest(myUniqueExceptionMessage);

        setupMockServiceToThrowException(expectedException);

        // ACT
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        List<ILoggingEvent> logsList = listAppender.list;
        ILoggingEvent lastLogEntry = logsList.get(logsList.size() - 1);
        assertThat(lastLogEntry.getLevel(), is(equalTo(Level.WARN)));
        assertThat(lastLogEntry.getMessage(), containsString(myUniqueExceptionMessage));
    }

    @Test
    public void handleSearchRequestException_shouldTrackExceptionToAppInsights() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "My unique BadSearchRequest exception message 3";
        BadSearchRequest expectedException = new BadSearchRequest(myUniqueExceptionMessage);

        setupMockServiceToThrowException(expectedException);

        // ACT
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        verify(appInsights, times(1)).trackException(expectedException);
    }

    @Test
    public void handleCaseValidationException_shouldReturnHttpErrorResponse_noDetailsDoesNotCauseError() throws Exception {

        // ARRANGE
        CaseValidationException expectedException = new CaseValidationException();

        setupMockServiceToThrowException(expectedException);

        // ACT
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        assertHttpErrorResponse(result, expectedException);
        assertExtraApiExceptionResponseProperties(result, expectedException);
        result.andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    public void handleCaseValidationException_shouldReturnHttpErrorResponse_badDetailsDoesNotCauseError() throws Exception {

        // ARRANGE
        CaseValidationException expectedException = new CaseValidationException();
        expectedException.withDetails("test details exist but in unrecognised format");
        setupObjectMapperWithDetails(expectedException.getDetails());

        setupMockServiceToThrowException(expectedException);

        // ACT
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        assertHttpErrorResponse(result, expectedException);
        assertExtraApiExceptionResponseProperties(result, expectedException);
        result.andExpect(jsonPath("$.details").exists());
    }

    @Test
    public void handleCaseValidationException_shouldReturnHttpErrorResponse_withValidationDetails() throws Exception {

        // ARRANGE
        CaseValidationException expectedException = new CaseValidationException();
        expectedException.withDetails("test details");
        setupObjectMapperWithDetails(expectedException.getDetails());

        setupMockServiceToThrowException(expectedException);

        // ACT
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        assertHttpErrorResponse(result, expectedException);
        assertExtraApiExceptionResponseProperties(result, expectedException);
        result.andExpect(jsonPath("$.details").exists());
    }

    @Test
    public void handleCaseValidationException_shouldLogExceptionAsWarning_withValidationDetails_includeFieldNamesButNotMessages() throws Exception {

        // ARRANGE
        CaseValidationException expectedException = new CaseValidationException();
        List<CaseFieldValidationError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new CaseFieldValidationError("field1", "response message 1"));
        fieldErrors.add(new CaseFieldValidationError("field2", "response message 2"));
        expectedException.withDetails(new CaseValidationError(fieldErrors));
        setupObjectMapperWithDetails(expectedException.getDetails());

        setupMockServiceToThrowException(expectedException);

        // ACT
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        List<ILoggingEvent> logsList = listAppender.list;
        ILoggingEvent lastLogEntry = logsList.get(logsList.size() - 1);
        assertThat(lastLogEntry.getLevel(), is(equalTo(Level.WARN)));
        assertThat(lastLogEntry.getFormattedMessage(), containsString(expectedException.getMessage()));
        // check logging field names
        assertThat(lastLogEntry.getFormattedMessage(), containsString(fieldErrors.get(0).getId()));
        assertThat(lastLogEntry.getFormattedMessage(), containsString(fieldErrors.get(1).getId()));
        // check not logging validation messages
        assertThat(lastLogEntry.getFormattedMessage(), not(containsString(fieldErrors.get(0).getMessage())));
        assertThat(lastLogEntry.getFormattedMessage(), not(containsString(fieldErrors.get(1).getMessage())));
    }

    @Test
    public void handleCaseValidationException_shouldTrackExceptionToAppInsightsAsWarning_withValidationDetails_includeFieldNamesButNotMessages()
        throws Exception {

        // ARRANGE
        CaseValidationException expectedException = new CaseValidationException();
        List<CaseFieldValidationError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new CaseFieldValidationError("field1", "response message 1"));
        fieldErrors.add(new CaseFieldValidationError("field2", "response message 2"));
        expectedException.withDetails(new CaseValidationError(fieldErrors));
        setupObjectMapperWithDetails(expectedException.getDetails());

        setupMockServiceToThrowException(expectedException);

        // ACT
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL));

        // ASSERT
        verify(appInsights, times(1)).trackException(eq(expectedException), customPropertiesCaptor.capture(), eq(SeverityLevel.Warning));
        // check logging field names
        Map<String, String> trackedCustomProperties = customPropertiesCaptor.getValue();
        String trackedFieldNames = trackedCustomProperties.get("CaseValidationError field IDs");
        assertThat(trackedFieldNames, containsString(fieldErrors.get(0).getId()));
        assertThat(trackedFieldNames, containsString(fieldErrors.get(1).getId()));
    }

    private void assertHttpErrorResponse(ResultActions result, Exception expectedException) throws Exception {

        // NB: as we cannot mock HttpError generate an equivalent and compare to response
        final HttpError<Serializable> expectedError = new HttpError<>(expectedException, mock(HttpServletRequest.class));

        // check the very basics
        result.andExpect(status().is(expectedError.getStatus()));
        result.andExpect(jsonPath("$.exception").value(expectedException.getClass().getName()));

        // check a bit more
        result.andExpect(jsonPath("$.message").value(expectedException.getMessage()));
    }

    private void assertExtraApiExceptionResponseProperties(ResultActions result, ApiException expectedException) throws Exception {

        // load extra properties
        Serializable exceptionDetails = expectedException.getDetails(); // NB: expecting test to be a simple string
        List<String> exceptionCallbackErrors = expectedException.getCallbackErrors();
        List<String> exceptionCallbackWarnings = expectedException.getCallbackWarnings();

        // check details
        if (exceptionDetails == null) {
            result.andExpect(jsonPath("$.details").doesNotExist());
        } else {
            result.andExpect(jsonPath("$.details").value(exceptionDetails));
        }

        // check callback errors
        if (exceptionCallbackErrors == null) {
            result.andExpect(jsonPath("$.callbackErrors").doesNotExist());
        } else {
            for (int i = 0; i < exceptionCallbackErrors.size(); i++) {
                result.andExpect(jsonPath(String.format("$.callbackErrors[%s]", i)).value(exceptionCallbackErrors.get(i)));
            }
        }

        // check callback warnings
        if (exceptionCallbackWarnings == null) {
            result.andExpect(jsonPath("$.callbackWarnings").doesNotExist());
        } else {
            for (int i = 0; i < exceptionCallbackWarnings.size(); i++) {
                result.andExpect(jsonPath(String.format("$.callbackWarnings[%s]", i)).value(exceptionCallbackWarnings.get(i)));
            }
        }
    }

    private void setupMockServiceToThrowException(Exception expectedException) {
        // configure chosen mock service to throw exception when controller is run
        when(mockService.execute(AccessControlService.CAN_READ)).thenThrow(expectedException);
    }

    private void setupObjectMapperWithDetails(Serializable details) {
        try {
            when(objectMapperService.convertObjectToString(details)).thenReturn(objectWriter.writeValueAsString(details));
        } catch (JsonProcessingException e) {
            Assert.fail("Unable to serialize details");
        }
    }
}
