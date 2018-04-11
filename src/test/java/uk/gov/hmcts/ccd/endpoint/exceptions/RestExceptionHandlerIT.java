package uk.gov.hmcts.ccd.endpoint.exceptions;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.endpoint.std.CaseDetailsEndpoint;

@RunWith(MockitoJUnitRunner.class)
public class RestExceptionHandlerIT {

    private static final String URL = "/caseworkers/uid/jurisdictions/jid/case-types/ctid/cases/cid/documents";
    private static final MediaType JSON_CONTENT_TYPE = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    private MockMvc mockMvc;

    @Mock
    private CaseDetailsEndpoint caseDetailsEndpoint;

    @Mock
    private AppInsights appInsights;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(caseDetailsEndpoint).setControllerAdvice(new RestExceptionHandler(appInsights)).build();
    }

    @DisplayName("CompletionException wrapping a ApiException should be unwrapped and handled as ApiException")
    @Test
    public void testCompletionExceptionsCausedByApiException() throws Exception {

        when(caseDetailsEndpoint.getDocumentsForEvent("jid", "ctid", "cid")).thenThrow(new CompletionException(new ApiException("test exception")));

        mockMvc.perform(get(URL).contentType(JSON_CONTENT_TYPE))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"));
    }

    @DisplayName("CompletionException not wrapping a ApiException should be unwrapped and handled as Exception")
    @Test
    public void testCompletionExceptionsCausedByNotApiException() throws Exception {

        when(caseDetailsEndpoint.getDocumentsForEvent("jid", "ctid", "cid")).thenThrow(new CompletionException(new IOException("test exception")));

        mockMvc.perform(get(URL).contentType(JSON_CONTENT_TYPE))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected Error"));
    }

    @DisplayName("api exceptions should be handled an the configured error code should be returned")
    @Test
    public void testApiExceptionsAreHandled() throws Exception {

        when(caseDetailsEndpoint.getDocumentsForEvent("jid", "ctid", "cid")).thenThrow(new BadRequestException("test exception"));

        mockMvc.perform(get(URL).contentType(JSON_CONTENT_TYPE))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @DisplayName("unknown exceptions should be handled as internal server error")
    @Test
    public void testUnknownExceptionsAreHandledAsInternalServerError() throws Exception {

        when(caseDetailsEndpoint.getDocumentsForEvent("jid", "ctid", "cid")).thenThrow(new RuntimeException("test exception"));

        mockMvc.perform(get(URL).contentType(JSON_CONTENT_TYPE))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unexpected Error"));
    }
}