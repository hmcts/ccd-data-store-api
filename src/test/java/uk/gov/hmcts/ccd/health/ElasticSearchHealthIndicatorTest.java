package uk.gov.hmcts.ccd.health;

import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.exception.CouldNotConnectException;
import io.searchbox.core.CatResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.actuate.health.Health.Builder;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;

class ElasticSearchHealthIndicatorTest {

    private static final String ES_HEALTH_RESPONSE_GREEN = "{\"status\": \"green\"}";
    private static final String ES_HEALTH_RESPONSE_RED = "{\"status\": \"red\"}";
    private static final String ES_HEALTH_RESPONSE_YELLOW = "{\"status\": \"yellow\"}";
    private static final String ES_INDICES_RESPONSE = "[\n"
        + "      {\n"
        + "        \"health\": \"green\",\n"
        + "        \"index\": \"testcomplexaddressbookcase_cases-000001\",\n"
        + "        \"docs.count\": \"0\",\n"
        + "        \"pri.store.size\": \"522b\"\n"
        + "      },\n"
        + "      {\n"
        + "        \"health\": \"green\",\n"
        + "        \"index\": \"aat_cases-000001\",\n"
        + "        \"docs.count\": \"1\",\n"
        + "        \"pri.store.size\": \"6.9kb\"\n"
        + "      }]";

    @Mock
    private JestClient client;

    @Mock
    private DefaultCaseDetailsRepository repository;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private ElasticSearchHealthIndicator healthIndicator;

    @Spy
    private Builder builder = new Builder();

    @Mock
    private CatResult indicesHealthResult;

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
        verify(repository, never()).getCasesCountByCaseType();
    }

    @Test
    public void statusIsProblemWhenException() throws IOException {
        when(client.execute(any())).thenThrow(new RuntimeException("test", new RuntimeException("test2")));

        healthIndicator.doHealthCheck(builder);

        verify(builder).status(ElasticSearchHealthIndicator.PROBLEM);
        verify(builder).withDetail("detail", "test");
    }

    @Test
    public void testStatusIsOutOfSyncWhenMissingIndex() throws Exception {

        stubESResponse(ES_HEALTH_RESPONSE_GREEN, ES_INDICES_RESPONSE);

        stubDatabase(
            new Object[]{"AAT", 22L},
            new Object[]{"GrantOfRepresentation", 14L}
        );

        healthIndicator.doHealthCheck(builder);

        JsonNode healthResultNode = mapper.readTree(healthResult.getJsonString());
        JsonNode indicesResultNode = mapper.readTree(indicesHealthResult.getJsonString());

        verify(builder).status(ElasticSearchHealthIndicator.OUT_OF_SYNC);
        verify(builder).withDetail("clusterHealth", healthResultNode);
        verify(builder).withDetail("clusterIndices", indicesResultNode);
    }

    @Test
    public void testStatusIsOutOfSyncWhenMissingCases() throws Exception {

        stubESResponse(ES_HEALTH_RESPONSE_GREEN, ES_INDICES_RESPONSE);

        stubDatabase(
            new Object[]{"AAT", 0L},
            new Object[]{"testcomplexaddressbookcase", 0L}
        );

        healthIndicator.doHealthCheck(builder);

        JsonNode healthResultNode = mapper.readTree(healthResult.getJsonString());
        JsonNode indicesResultNode = mapper.readTree(indicesHealthResult.getJsonString());

        verify(builder).status(ElasticSearchHealthIndicator.OUT_OF_SYNC);
        verify(builder).withDetail("clusterHealth", healthResultNode);
        verify(builder).withDetail("clusterIndices", indicesResultNode);
    }

    @Test
    public void testStatusIsUpWhenClusterHealthIsGreenAndInSync() throws Exception {

        stubESResponse(ES_HEALTH_RESPONSE_GREEN, ES_INDICES_RESPONSE);

        stubDatabase(
            new Object[]{"AAT", 1L},
            new Object[]{"testcomplexaddressbookcase", 0L}
        );

        healthIndicator.doHealthCheck(builder);

        JsonNode healthResultNode = mapper.readTree(healthResult.getJsonString());
        JsonNode indicesResultNode = mapper.readTree(indicesHealthResult.getJsonString());

        verify(builder).up();
        verify(builder).withDetail("clusterHealth", healthResultNode);
        verify(builder).withDetail("clusterIndices", indicesResultNode);
    }

    @Test
    public void testStatusProblems() throws Exception {

        stubESResponse(ES_HEALTH_RESPONSE_RED, ES_INDICES_RESPONSE);

        stubDatabase(
            new Object[]{"AAT", 1L},
            new Object[]{"testcomplexaddressbookcase", 0L}
        );

        healthIndicator.doHealthCheck(builder);

        verify(builder).status(ElasticSearchHealthIndicator.PROBLEM);
    }

    @Test
    public void testStatusIsUpWhenClusterHealthIsYellowAndInSync() throws Exception {

        stubESResponse(ES_HEALTH_RESPONSE_YELLOW, ES_INDICES_RESPONSE);

        stubDatabase(
            new Object[]{"AAT", 1L},
            new Object[]{"testcomplexaddressbookcase", 0L}
        );

        healthIndicator.doHealthCheck(builder);

        verify(builder).up();
    }

    private void stubDatabase(Object[] row1, Object[] row2) {
        when(repository.getCasesCountByCaseType()).thenReturn(newArrayList(row1, row2));
    }

    private void stubESResponse(String healthResponse, String indicesResponse) throws IOException {
        JsonParser parser = new JsonParser();
        JsonObject healthJsonObject = parser.parse(healthResponse).getAsJsonObject();
        when(healthResult.getJsonObject()).thenReturn(healthJsonObject);
        when(healthResult.getJsonString()).thenReturn(healthResponse);
        when(indicesHealthResult.getJsonString()).thenReturn(indicesResponse);
        when(client.execute(any())).thenReturn(healthResult, indicesHealthResult);
    }
}
