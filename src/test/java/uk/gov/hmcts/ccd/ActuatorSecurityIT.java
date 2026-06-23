package uk.gov.hmcts.ccd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ActuatorSecurityIT extends WireMockBaseTest {

    private MockMvc mockMvc;

    @Inject
    private WebApplicationContext wac;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void shouldAllowAnonymousAccessToHealthEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/health")).andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == 200 || status == 503,
            "Expected /health to return 200 or 503, but got: " + status);
    }

    @Test
    void shouldNotExposeLoggersEndpointAnonymously() throws Exception {
        MvcResult result = mockMvc.perform(get("/loggers")).andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status == 401 || status == 404,
            "Expected /loggers to be protected (401) or disabled (404), but got: " + status);
    }
}
