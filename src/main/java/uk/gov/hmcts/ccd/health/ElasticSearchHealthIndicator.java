package uk.gov.hmcts.ccd.health;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.exception.CouldNotConnectException;
import io.searchbox.cluster.Health;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "search.elastic.enabled")
@Slf4j
public class ElasticSearchHealthIndicator extends AbstractHealthIndicator {

    protected static final String COULD_NOT_CONNECT = "COULD_NOT_CONNECT";
    protected static final String PROBLEM = "PROBLEM";
    private JestClient client;
    private ObjectMapper objectMapper;

    @Autowired
    public ElasticSearchHealthIndicator(JestClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doHealthCheck(Builder builder) {
        try {
            JestResult healthResult = getClusterHealth();
            builder.withDetail("clusterHealth", asJsonNode(healthResult));

            switch (extractStatus(healthResult)) {
                case "green":
                case "yellow":
                    builder.up();
                    break;
                case "red":
                default:
                    builder.status(PROBLEM);
            }
        } catch (CouldNotConnectException e) {
            builder.status(COULD_NOT_CONNECT);
        } catch (Exception e) {
            builder.status(PROBLEM);
            log.warn("problem checking ES health: ", e);
            if (e.getMessage() != null) {
                builder.withDetail("detail", e.getMessage());
            }
        }
    }

    private String extractStatus(JestResult healthResult) {
        return healthResult.getJsonObject().get("status").getAsString();
    }

    private JsonNode asJsonNode(JestResult jestResult) throws IOException {
        return objectMapper.readTree(jestResult.getJsonString());
    }

    private JestResult getClusterHealth() throws IOException {
        Health health = new Health.Builder().build();
        return client.execute(health);
    }
}
