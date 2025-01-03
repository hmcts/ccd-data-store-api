package uk.gov.hmcts.ccd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import uk.gov.hmcts.ccd.test.ElasticsearchIndexSettings;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@Slf4j
public class ElasticsearchITSetup {

    private static final String DATA_DIR = "elasticsearch/data";
    private static final String CONFIG_DIR = "elasticsearch/config";
    private static final String INDEX_SETTINGS = "classpath:" + CONFIG_DIR + "/index-settings.json";
    public static final String INDEX_TYPE = "_doc";
    public static final String[] INDICES = {"aat_cases", "mapper_cases", "security_cases",
        "restricted_security_cases", "global_search"};

    @Value("${search.elastic.port}")
    private int httpPortValue;

    private String url;

    private PortableHttpClient httpClient;

    public ElasticsearchITSetup(int httpPortValue) {
        this.httpPortValue = httpPortValue;
        this.httpClient = new PortableHttpClient();
        this.url = "http://localhost:" + httpPortValue;
        waitForClusterYellow(3);
    }

    private void waitForClusterYellow(int tries) {
        for (int i = 0; i < 5; i++) {
            HttpGet request = new HttpGet(url("/_cluster/health?wait_for_status=yellow&timeout=50s"));
            httpClient.execute(request, (Consumer<CloseableHttpResponse>) response -> {
                    if (response.getStatusLine().getStatusCode() != 200) {
                        Assertions.assertTrue(tries > 0,
                            "Cluster does not reached yellow status in specified timeout");
                        waitForClusterYellow(tries - 1);
                    }
                }

            );
        }
    }

    private String url(String path) {
        return url + path;
    }

    // * * * * * * * * * * * * * * Initializing indexes * * * * * * * * * * * * * * * * * * * * * * * *

    public void initIndexes() throws IOException {
        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

        for (String idx : INDICES) {
            ElasticsearchIndexSettings settings = new ElasticsearchIndexSettings(
                Optional.of(resourceResolver.getResource(INDEX_SETTINGS).getInputStream()), Optional.empty()
            );

            settings.addType(INDEX_TYPE, resourceResolver
                .getResource(String.format("classpath:%s/mappings-%s.json", CONFIG_DIR, idx))
                .getInputStream());

            createIndex(idx, settings);
        }
    }

    void createIndex(String indexName, ElasticsearchIndexSettings settings) {
        if (!indexExists(indexName)) {
            HttpPut request = new HttpPut(url("/" + indexName));
            request.setEntity(new StringEntity(settings.toJson().toString(), APPLICATION_JSON));
            httpClient.execute(request, response -> {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String responseBody = "";
                    try {
                        responseBody = ElasticsearchIndexSettings.unwrapIO(response.getEntity().getContent());
                    } catch (IOException e) {
                        log.error("Error during reading response body", e);
                        responseBody = "!! Failed to get the response boy !!";
                    }
                    throw new RuntimeException("Call to elasticsearch resulted in error:\n" + responseBody);
                }
            });
            waitForClusterYellow(3);
        }
    }

    private boolean indexExists(String indexName) {
        HttpHead request = new HttpHead(url("/" + indexName));
        return httpClient.execute(request, response -> response.getStatusLine().getStatusCode() == 200);
    }

    // * * * * * * * * * * * * * * Initializing data * * * * * * * * * * * * * * * * * * * * * * * *

    public void initData() throws IOException {
        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

        for (String idx : INDICES) {
            Resource[] resources =
                resourceResolver.getResources(String.format("classpath:%s/%s/*.json", DATA_DIR, idx));
            for (Resource resource : resources) {
                String caseString = IOUtils.toString(resource.getInputStream(), UTF_8);
                doIndex(idx, INDEX_TYPE, caseString);
            }
        }
    }

    private void doIndex(String indexName, String indexType, String... jsonArray) {
        String bulkRequestBody = Arrays.stream(jsonArray)
            .flatMap(json ->
                Stream.of(
                    indexMetadataJson(indexName, indexType, null, null), json
                )
            )
            .map((jsonNodes) -> jsonNodes.replace('\n', ' ').replace('\r', ' '))
            .collect(joining("\n")) + "\n";

        performBulkRequest(url("/_bulk"), bulkRequestBody);
    }

    private String indexMetadataJson(String indexName, String indexType, String id, String routing) {
        StringJoiner joiner = new StringJoiner(",");
        if (indexName != null) {
            joiner.add("\"_index\": \"" + indexName + "\"");
        }
        if (indexType != null) {
            joiner.add("\"_type\": \"" + indexType + "\"");
        }
        if (id != null) {
            joiner.add("\"_id\": \"" + id + "\"");
        }
        if (routing != null) {
            joiner.add("\"_routing\": \"" + routing + "\"");
        }
        return "{ \"index\": {" + joiner + "} }";
    }

    private void performBulkRequest(String requestUrl, String bulkRequestBody) {
        HttpPost request = new HttpPost(requestUrl);
        request.setHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"));
        request.setEntity(new StringEntity(bulkRequestBody, UTF_8));
        httpClient.execute(request, (r) -> {
            waitForClusterYellow(3);
        });
    }

}
