package uk.gov.hmcts.ccd.health;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.exception.CouldNotConnectException;
import io.searchbox.cluster.Health;
import io.searchbox.core.Cat;
import io.searchbox.core.CatResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;

@Component
@Slf4j
public class ElasticSearchHealthIndicator extends AbstractHealthIndicator {

    protected static final String COULD_NOT_CONNECT = "COULD_NOT_CONNECT";
    protected static final String PROBLEM = "PROBLEM";
    protected static final String OUT_OF_SYNC = "OUT_OF_SYNC";
    private JestClient client;
    private DefaultCaseDetailsRepository caseDetailsRepository;
    private ObjectMapper objectMapper;

    @Autowired
    public ElasticSearchHealthIndicator(JestClient client, DefaultCaseDetailsRepository caseDetailsRepository, ObjectMapper objectMapper) {
        this.client = client;
        this.caseDetailsRepository = caseDetailsRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doHealthCheck(Builder builder) {
        try {
            JestResult healthResult = getClusterHealth();
            builder.withDetail("clusterHealth", asJsonNode(healthResult));

            JestResult indicesHealthResult = getClusterIndicesHealth();
            builder.withDetail("clusterIndices", asJsonNode(indicesHealthResult));

            boolean isOutOfSync = buildIndicesStats(builder, indicesHealthResult);

            if (isOutOfSync) {
                builder.status(OUT_OF_SYNC);
            } else {
                switch (extractStatus(healthResult)) {
                    case "green":
                    case "yellow":
                        builder.up();
                        break;
                    case "red":
                    default:
                        builder.status(PROBLEM);
                }
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

    private boolean buildIndicesStats(Builder builder, JestResult indicesHealthResult) {
        List<Index> indices = toIndexList(indicesHealthResult);
        List<Object[]> casesCountByCaseType = caseDetailsRepository.getCasesCountByCaseType();

        boolean isOutOfSync = false;
        for (Object[] row : casesCountByCaseType) {
            String caseType = (String) row[0];
            String ccdCasesCount = Long.toString((Long) row[1]);
            Optional<Index> indexOpt = findIndexForCaseType(indices, caseType);
            String esCasesCount = indexOpt.map(index -> index.getCount()).orElse("-");
            builder.withDetail(caseType + " cases CCD/ES", ccdCasesCount + "/" + esCasesCount);

            if (!indexOpt.isPresent() || !esCasesCount.equals(ccdCasesCount)) {
                isOutOfSync = true;
            }
        }
        return isOutOfSync;
    }

    private String extractStatus(JestResult healthResult) {
        return healthResult.getJsonObject().get("status").getAsString();
    }

    private Optional<Index> findIndexForCaseType(List<Index> indices, String caseType) {
        return indices.stream().filter(i ->
                        i.index.split("_cases")[0].equals(caseType.toLowerCase())
                    ).findAny();
    }

    private List<Index> toIndexList(JestResult indicesHealthResult) {
        Gson gson = new Gson();
        return newArrayList(gson.fromJson(indicesHealthResult.getJsonString(), Index[].class));
    }

    private CatResult getClusterIndicesHealth() throws IOException {
        return client.execute(new Cat.IndicesBuilder().build());
    }

    private JsonNode asJsonNode(JestResult jestResult) throws IOException {
        return objectMapper.readTree(jestResult.getJsonString());
    }

    private JestResult getClusterHealth() throws IOException {
        Health health = new Health.Builder().build();
        return client.execute(health);
    }

    private static class Index {
        private String index;
        @SerializedName("docs.count")
        private int count;

        public String getCount() {
            return Integer.toString(count);
        }
    }
}
