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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tracks successful case creation and removes those cases' assignments after each BEFTA scenario. */
final class RoleAssignmentCleanup {

    private static final Logger LOG = LoggerFactory.getLogger(RoleAssignmentCleanup.class);
    private static final String CREATOR = "[CREATOR]";
    private static final int CLEANUP_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MILLIS = 250;
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(408, 429, 500, 502, 503, 504);
    private static final ThreadLocal<RoleAssignmentCleanup> CURRENT = new ThreadLocal<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // Application Gateway rejects cleartext HTTP/2 upgrade requests with HTTP 403.
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(10)).build();
    private static final Pattern CREATION_PATH = Pattern.compile(
        "/(?:case-types/[^/]+/cases|(?:caseworkers|citizens)/[^/]+/jurisdictions/[^/]+/case-types/[^/]+/cases)/?"
    );
    private static final Pattern USER_ROLES_PATH = Pattern.compile("/cases/([^/]+)/users/([^/]+)/?");
    private static final Filter TRACKER = (request, specification, context) -> {
        Response response = context.next(request, specification);
        RoleAssignmentCleanup cleanup = CURRENT.get();
        if (cleanup != null) {
            cleanup.track(request, response);
        }
        return response;
    };

    private final String testUrl;
    private final URI endpoint;
    private final Set<String> caseReferences = new LinkedHashSet<>();
    private final Map<CaseUser, UserAssignments> assignmentsByUser = new HashMap<>();

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
        String method = request.getMethod();
        int status = response.statusCode();
        boolean created = "POST".equalsIgnoreCase(method) && status == 201;
        boolean deleted = "DELETE".equalsIgnoreCase(method) && status == 200;
        boolean replaced = "PUT".equalsIgnoreCase(method) && status == 204;
        if (!created && !deleted && !replaced) {
            return;
        }
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
        Matcher userRoles = USER_ROLES_PATH.matcher(path);
        try {
            if (created && CREATION_PATH.matcher(path).matches()) {
                trackCreatedCase(response);
            } else if ((created || deleted)
                && ("/case-users".equals(path) || "/case-users/".equals(path)) && !caseReferences.isEmpty()) {
                trackAssignments(request.getBody(), created);
            } else if (replaced && userRoles.matches() && caseReferences.contains(userRoles.group(1))) {
                CaseUser user = new CaseUser(userRoles.group(1), userRoles.group(2));
                UserAssignments current = assignmentsByUser.computeIfAbsent(user, key -> new UserAssignments());
                current.roles.clear();
                for (JsonNode role : readBody(request.getBody()).path("case_roles")) {
                    if (!CREATOR.equalsIgnoreCase(role.asText())) {
                        current.roles.add(role.asText().toLowerCase(Locale.ROOT));
                    }
                }
                if (current.roles.isEmpty()) {
                    assignmentsByUser.remove(user);
                }
            }
        } catch (IOException | RuntimeException e) {
            throw new FunctionalTestException("Could not track a successful case or case-role assignment response");
        }
    }

    private void trackCreatedCase(Response response) throws IOException {
        String content = response.asString();
        JsonNode body = content.isBlank() ? null : MAPPER.readTree(content);
        JsonNode id = body == null ? null : body.get("id");
        // Creation succeeds without disclosing case details when the user lacks read access.
        if (body == null || body.isNull() || (id != null && id.isNull())) {
            LOG.warn("Cannot track role assignment cleanup: case creation returned no case reference");
            return;
        }
        if (id == null || !(id.isTextual() || id.isIntegralNumber()) || !id.asText().matches("[0-9]{16}")) {
            throw new IllegalArgumentException();
        }
        caseReferences.add(id.asText());
    }

    private void trackAssignments(Object body, boolean adding) throws IOException {
        // CCD credits organisations only for users who had no non-creator roles before this request.
        Set<CaseUser> newUsersInBatch = new HashSet<>();
        for (JsonNode assignment : readBody(body).path("case_users")) {
            String caseId = assignment.path("case_id").asText("");
            String userId = assignment.path("user_id").asText("");
            String role = assignment.path("case_role").asText("").toLowerCase(Locale.ROOT);
            if (!caseReferences.contains(caseId) || userId.isBlank() || role.isBlank()
                || CREATOR.equalsIgnoreCase(role)) {
                continue;
            }
            CaseUser user = new CaseUser(caseId, userId);
            UserAssignments current = assignmentsByUser.computeIfAbsent(user, key -> new UserAssignments());
            if (adding) {
                if (current.roles.isEmpty()) {
                    newUsersInBatch.add(user);
                }
                current.roles.add(role);
                String organisation = assignment.path("organisation_id").asText("");
                if (newUsersInBatch.contains(user) && !organisation.isBlank()) {
                    current.countedOrganisations.add(organisation);
                }
            } else {
                current.roles.remove(role);
                if (current.roles.isEmpty()) {
                    assignmentsByUser.remove(user);
                }
            }
        }
    }

    private static JsonNode readBody(Object body) throws IOException {
        if (body instanceof String) {
            return MAPPER.readTree((String) body);
        } else if (body instanceof byte[]) {
            return MAPPER.readTree((byte[]) body);
        }
        return MAPPER.valueToTree(body);
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
                cleanUpCase(caseReference, headers);
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

    private void cleanUpCase(String caseReference, Map<String, String> headers)
        throws IOException, InterruptedException {
        for (int attempt = 1; attempt <= CLEANUP_ATTEMPTS; attempt++) {
            try {
                // Reconcile before every DELETE: an earlier response may have failed after deletion committed.
                List<Map<String, String>> assignments = findAssignments(caseReference, headers);
                if (assignments.isEmpty()) {
                    return;
                }
                send("/case-users", "DELETE", Map.of("case_users", assignments), headers);
                if (findAssignments(caseReference, headers).isEmpty()) {
                    return;
                }
                throw new RetryableCleanupException("Assignments remain after deletion");
            } catch (RetryableCleanupException e) {
                if (attempt == CLEANUP_ATTEMPTS) {
                    throw e;
                }
                LOG.warn("Retrying role assignment cleanup for case {} after attempt {}/{}: {}",
                    caseReference, attempt, CLEANUP_ATTEMPTS, e.getMessage());
                Thread.sleep(RETRY_DELAY_MILLIS);
            }
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
        Set<Assignment> seen = new HashSet<>();
        Set<CaseUser> countedUsers = new HashSet<>();
        List<Map<String, String>> result = new ArrayList<>();
        for (JsonNode assignment : assignments) {
            String caseId = text(assignment, "case_id");
            String userId = text(assignment, "user_id");
            String role = text(assignment, "case_role");
            if (!caseReference.equals(caseId) || userId.isBlank() || !role.matches("\\[.+]")) {
                throw new FunctionalTestException("Search returned an invalid or unowned case-user-role tuple");
            }
            if (!seen.add(new Assignment(caseId, userId, role))) {
                continue;
            }
            CaseUser user = new CaseUser(caseId, userId);
            UserAssignments current = assignmentsByUser.get(user);
            if (current != null && !current.countedOrganisations.isEmpty()
                && !CREATOR.equalsIgnoreCase(role) && countedUsers.add(user)) {
                for (String organisation : current.countedOrganisations) {
                    result.add(Map.of("case_id", caseId, "user_id", userId, "case_role", role,
                                      "organisation_id", organisation));
                }
            } else {
                result.add(Map.of("case_id", caseId, "user_id", userId, "case_role", role));
            }
        }
        return result;
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
        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RetryableCleanupException(method + " " + path + " failed ("
                + e.getClass().getSimpleName() + ")");
        }
        LOG.info("Role assignment cleanup response: {} {} status={} body={}",
            method, uri, response.statusCode(), response.body());
        if (response.statusCode() != 200) {
            String message = method + " " + path + " returned HTTP " + response.statusCode();
            if (RETRYABLE_STATUS_CODES.contains(response.statusCode())) {
                throw new RetryableCleanupException(message);
            }
            throw new FunctionalTestException(message);
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

    private record CaseUser(String caseId, String userId) {
    }

    private static final class UserAssignments {
        private final Set<String> roles = new HashSet<>();
        private final Set<String> countedOrganisations = new LinkedHashSet<>();
    }

    private static final class RetryableCleanupException extends FunctionalTestException {

        private static final long serialVersionUID = 1L;

        private RetryableCleanupException(String message) {
            super(message);
        }
    }
}
