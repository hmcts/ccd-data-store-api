package uk.gov.hmcts.ccd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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

    @Test
    void shouldAllowAnonymousAccessToMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/metrics"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldExposeCacheHitMissAndPutMetrics() throws Exception {
        // Exercise a known cache to generate put/hit/miss stats.
        var cache = cacheManager.getCache("userInfoCache");
        assertTrue(cache != null, "Expected userInfoCache to exist");

        cache.put("metrics-test-key", "metrics-test-value");
        cache.get("metrics-test-key"); // hit
        cache.get("metrics-test-miss"); // miss

        MvcResult getsMetricResult = mockMvc.perform(get("/metrics/cache.gets"))
            .andExpect(status().isOk())
            .andReturn();
        String getsMetricResponse = getsMetricResult.getResponse().getContentAsString();
        assertTrue(getsMetricResponse.contains("\"result\""),
            "Expected cache.gets metric to include hit/miss result tags");
        assertTrue(getsMetricResponse.contains("hit") || getsMetricResponse.contains("miss"),
            "Expected cache.gets metric to include hit or miss tags");

        mockMvc.perform(get("/metrics/cache.hits"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/metrics/cache.misses"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/metrics/cache.loads"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/metrics/cache.eviction"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/metrics/cache.average"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/metrics/cache.of"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/metrics/cache.hitrate"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/metrics/cache.puts"))
            .andExpect(status().isOk());
    }
}
