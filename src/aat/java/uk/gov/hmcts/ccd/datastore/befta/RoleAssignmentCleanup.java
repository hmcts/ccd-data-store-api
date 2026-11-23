package uk.gov.hmcts.ccd.datastore.befta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.befta.TestAutomationAdapter;
import uk.gov.hmcts.befta.auth.UserTokenProviderConfig;
import uk.gov.hmcts.befta.data.UserData;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.util.BeftaUtils;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.befta.util.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/** Tracks successful case creation and removes those cases' assignments after each BEFTA scenario. */
final class RoleAssignmentCleanup {

    private static final Logger LOG = LoggerFactory.getLogger(RoleAssignmentCleanup.class);
    private static final ThreadLocal<RoleAssignmentCleanup> CURRENT = new ThreadLocal<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // Application Gateway rejects cleartext HTTP/2 upgrade requests with HTTP 403.
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(10)).build();
    private static final Pattern CREATION_PATH = Pattern.compile(
        "/(?:case-types/[^/]+/cases|(?:caseworkers|citizens)/[^/]+/jurisdictions/[^/]+/case-types/[^/]+/cases)/?"
    );
    private static final Filter TRACKER = (request, specification, context) -> {
        Response response = context.next(request, specification);
        RoleAssignmentCleanup cleanup = CURRENT.get();
        if (cleanup != null && "POST".equalsIgnoreCase(request.getMethod()) && response.statusCode() == 201) {
            cleanup.track(request, response);
        }
        return response;
    };

    private final String testUrl;
    private final URI endpoint;
    private final Set<String> caseReferences = new LinkedHashSet<>();
    private final Map<Assignment, String> organisations = new HashMap<>();

    private RoleAssignmentCleanup(String testUrl) {
        this.testUrl = testUrl.replaceAll("/+$", "");
        this.endpoint = URI.create(this.testUrl).normalize();
        if (endpoint.getHost() == null || endpoint.getRawUserInfo() != null
            || endpoint.getRawQuery() != null || endpoint.getRawFragment() != null
            || !("http".equalsIgnoreCase(endpoint.getScheme())
            || "https".equalsIgnoreCase(endpoint.getScheme()))) {
            throw new IllegalArgumentException("Cleanup requires an absolute HTTP(S) test URL");
        }
    }

    static synchronized void install() {
        if (!RestAssured.filters().contains(TRACKER)) {
            RestAssured.filters(TRACKER);
        }
    }

    static void begin(String testUrl) {
        CURRENT.set(new RoleAssignmentCleanup(testUrl));
    }

    static void finish(TestAutomationAdapter adapter) {
        RoleAssignmentCleanup cleanup = CURRENT.get();
        CURRENT.remove();
        if (cleanup != null && !cleanup.caseReferences.isEmpty()) {
            cleanup.cleanUp(adapter);
        }
    }

    private void track(FilterableRequestSpecification request, Response response) {
        URI actual = URI.create(request.getURI()).normalize();
        if (!endpoint.getScheme().equalsIgnoreCase(actual.getScheme())
            || !endpoint.getHost().equalsIgnoreCase(actual.getHost()) || port(endpoint) != port(actual)) {
            return;
        }
        String basePath = endpoint.getRawPath();
        if (!actual.getRawPath().startsWith(basePath + "/")) {
            return;
        }
        String path = actual.getRawPath().substring(basePath.length());
        try {
            if (CREATION_PATH.matcher(path).matches()) {
                String content = response.asString();
                JsonNode body = content.isBlank() ? null : MAPPER.readTree(content);
                JsonNode id = body == null ? null : body.get("id");
                // Creation succeeds without disclosing case details when the user lacks read access.
                if (body == null || body.isNull() || (id != null && id.isNull())) {
                    LOG.warn("Cannot track role assignment cleanup: case creation returned no case reference");
                    return;
                }
                if (id == null || !(id.isTextual() || id.isIntegralNumber())
                    || !id.asText().matches("[0-9]{16}")) {
                    throw new IllegalArgumentException();
                }
                caseReferences.add(id.asText());
            } else if (("/case-users".equals(path) || "/case-users/".equals(path)) && !caseReferences.isEmpty()) {
                trackOrganisations(request.getBody());
            }
        } catch (IOException | RuntimeException e) {
            throw new FunctionalTestException("Could not track a successful case or case-role assignment response");
        }
    }

    private void trackOrganisations(Object body) throws IOException {
        JsonNode json;
        if (body instanceof String) {
            json = MAPPER.readTree((String) body);
        } else if (body instanceof byte[]) {
            json = MAPPER.readTree((byte[]) body);
        } else {
            json = MAPPER.valueToTree(body);
        }
        JsonNode assignments = json == null ? null : json.get("case_users");
        if (assignments != null && assignments.isArray()) {
            for (JsonNode assignment : assignments) {
                String caseId = assignment.path("case_id").asText("");
                String userId = assignment.path("user_id").asText("");
                String role = assignment.path("case_role").asText("");
                String organisation = assignment.path("organisation_id").asText("");
                if (caseReferences.contains(caseId) && !userId.isBlank() && !role.isBlank()
                    && !organisation.isBlank()) {
                    organisations.putIfAbsent(new Assignment(caseId, userId, role), organisation);
                }
            }
        }
    }

    private void cleanUp(TestAutomationAdapter adapter) {
        Map<String, String> headers;
        try {
            headers = authenticate(adapter);
        } catch (IOException | ExecutionException | RuntimeException e) {
            throw new FunctionalTestException("Could not authenticate role assignment cleanup");
        }
        List<String> failures = new ArrayList<>();
        for (String caseReference : caseReferences) {
            try {
                List<Map<String, String>> assignments = findAssignments(caseReference, headers);
                if (!assignments.isEmpty()) {
                    send("/case-users", "DELETE", Map.of("case_users", assignments), headers);
                    if (!findAssignments(caseReference, headers).isEmpty()) {
                        throw new FunctionalTestException("Assignments remain after deletion");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failures.add("Case " + caseReference + ": cleanup interrupted");
                break;
            } catch (FunctionalTestException e) {
                failures.add("Case " + caseReference + ": " + e.getMessage());
            } catch (IOException | RuntimeException e) {
                failures.add("Case " + caseReference + ": cleanup failed (" + e.getClass().getSimpleName() + ")");
            }
        }
        if (!failures.isEmpty()) {
            throw new FunctionalTestException("Role assignment cleanup failed: " + String.join("; ", failures));
        }
    }

    private List<Map<String, String>> findAssignments(String caseReference, Map<String, String> headers)
        throws IOException, InterruptedException {
        HttpResponse<String> response = send("/case-users/search", "POST",
            Map.of("case_ids", List.of(caseReference), "user_ids", List.of()), headers);
        JsonNode root = MAPPER.readTree(response.body());
        JsonNode assignments = root == null ? null : root.get("case_users");
        if (assignments == null || !assignments.isArray()) {
            throw new FunctionalTestException("Search response has no case_users array");
        }
        Map<Assignment, Map<String, String>> unique = new LinkedHashMap<>();
        for (JsonNode assignment : assignments) {
            String caseId = text(assignment, "case_id");
            String userId = text(assignment, "user_id");
            String role = text(assignment, "case_role");
            if (!caseReference.equals(caseId) || userId.isBlank() || !role.matches("\\[.+]")) {
                throw new FunctionalTestException("Search returned an invalid or unowned case-user-role tuple");
            }
            Assignment key = new Assignment(caseId, userId, role);
            Map<String, String> tuple = new LinkedHashMap<>(Map.of(
                "case_id", caseId, "user_id", userId, "case_role", role));
            if (organisations.containsKey(key)) {
                tuple.put("organisation_id", organisations.get(key));
            }
            unique.putIfAbsent(key, tuple);
        }
        return new ArrayList<>(unique.values());
    }

    private HttpResponse<String> send(String path, String method, Map<String, ?> body, Map<String, String> headers)
        throws IOException, InterruptedException {
        URI uri = URI.create(testUrl + path);
        String payload = MAPPER.writeValueAsString(body);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(payload));
        headers.forEach(request::header);
        LOG.info("Role assignment cleanup request: {} {} body={}", method, uri, payload);
        HttpResponse<String> response = HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
        LOG.info("Role assignment cleanup response: {} {} status={} body={}",
            method, uri, response.statusCode(), response.body());
        if (response.statusCode() != 200) {
            throw new FunctionalTestException(method + " " + path + " returned HTTP " + response.statusCode());
        }
        return response;
    }

    private static Map<String, String> authenticate(TestAutomationAdapter adapter)
        throws IOException, ExecutionException {
        JsonNode fixture = JsonUtils.readObjectFromJsonFile(BeftaUtils.getFileFromResource(
            "features/common/users/BeftaCaseworkerCaa.td.json").getPath(), JsonNode.class);
        String password = EnvironmentVariableUtils.resolvePossibleVariable(fixture.path("password").asText());
        if (password == null || password.isBlank() || password.contains("[[$")) {
            throw new IllegalArgumentException();
        }
        UserData user = new UserData(fixture.path("username").asText(), password);
        adapter.authenticate(user, UserTokenProviderConfig.DEFAULT_INSTANCE.getClientId());
        String token = user.getAccessToken();
        String serviceToken = adapter.getNewS2SToken("aac_manage_case_assignment");
        if (token == null || token.isBlank() || serviceToken == null || serviceToken.isBlank()) {
            throw new IllegalArgumentException();
        }
        return Map.of("Authorization", "Bearer " + token, "ServiceAuthorization", serviceToken);
    }

    private static int port(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static String text(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null && value.isTextual() ? value.textValue() : "";
    }

    private record Assignment(String caseId, String userId, String role) {
    }
}
