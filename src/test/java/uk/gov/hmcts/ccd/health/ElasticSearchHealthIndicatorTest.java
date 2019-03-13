package uk.gov.hmcts.ccd.health;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.exception.CouldNotConnectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.actuate.health.Health.Builder;

class ElasticSearchHealthIndicatorTest {

    private static final String ES_HEALTH_RESPONSE_GREEN = "{\"status\": \"green\"}";
    private static final String ES_HEALTH_RESPONSE_RED = "{\"status\": \"red\"}";
    private static final String ES_HEALTH_RESPONSE_YELLOW = "{\"status\": \"yellow\"}";

    @Mock
    private JestClient client;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private ElasticSearchHealthIndicator healthIndicator;

    @Spy
    private Builder builder = new Builder();

    @Mock
    private JestResult healthResult;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void statusIsCannotConnectWhenCannotConnectException() throws Exception {
        when(client.execute(any())).thenThrow(new CouldNotConnectException("test", new RuntimeException()));

        healthIndicator.doHealthCheck(builder);

        verify(builder).status(ElasticSearchHealthIndicator.COULD_NOT_CONNECT);
    }

    @Test
    public void statusIsProblemWhenException() throws IOException {
        when(client.execute(any())).thenThrow(new RuntimeException("test", new RuntimeException("test2")));

        healthIndicator.doHealthCheck(builder);

        verify(builder).status(ElasticSearchHealthIndicator.PROBLEM);
        verify(builder).withDetail("detail", "test");
    }

    @Test
    public void testStatusIsUpWhenClusterHealthIsGreen() throws Exception {

        stubESResponse(ES_HEALTH_RESPONSE_GREEN);

        healthIndicator.doHealthCheck(builder);

        JsonNode healthResultNode = mapper.readTree(healthResult.getJsonString());

        verify(builder).up();
        verify(builder).withDetail("clusterHealth", healthResultNode);
    }

    @Test
    public void testStatusProblems() throws IOException {

        stubESResponse(ES_HEALTH_RESPONSE_RED);

        healthIndicator.doHealthCheck(builder);

        verify(builder).status(ElasticSearchHealthIndicator.PROBLEM);
    }

    @Test
    public void testStatusIsUpWhenClusterHealthIsYellow() throws IOException {

        stubESResponse(ES_HEALTH_RESPONSE_YELLOW);

        healthIndicator.doHealthCheck(builder);

        verify(builder).up();
    }


    private void stubESResponse(String healthResponse) throws IOException {
        JsonParser parser = new JsonParser();
        JsonObject healthJsonObject = parser.parse(healthResponse).getAsJsonObject();
        when(healthResult.getJsonObject()).thenReturn(healthJsonObject);
        when(healthResult.getJsonString()).thenReturn(healthResponse);
        when(client.execute(any())).thenReturn(healthResult);
    }
}
