package uk.gov.hmcts.ccd.datastore.befta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import uk.gov.hmcts.befta.TestAutomationAdapter;
import uk.gov.hmcts.befta.auth.UserTokenProviderConfig;
import uk.gov.hmcts.befta.data.UserData;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoleAssignmentCleanupTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FIRST = "1234567890123456";
    private static final String SECOND = "1234567890123457";
    private static final String EXISTING = "1234567890123458";
    private final Map<String, List<Map<String, String>>> roles = new ConcurrentHashMap<>();
    private final List<List<String>> searches = new CopyOnWriteArrayList<>();
    private final List<Map<String, String>> deletions = new CopyOnWriteArrayList<>();
    private final Queue<Integer> searchStatuses = new ConcurrentLinkedQueue<>();
    private final Queue<Integer> deleteStatuses = new ConcurrentLinkedQueue<>();
    private final Queue<Integer> committedDeleteStatuses = new ConcurrentLinkedQueue<>();
    private final AtomicInteger deleteCalls = new AtomicInteger();
    private final AtomicReference<Throwable> serverFailure = new AtomicReference<>();
    private final TestAutomationAdapter adapter = mock(TestAutomationAdapter.class);
    private HttpServer server;
    private String url;
    private List<Filter> previousFilters;
    private MockedStatic<EnvironmentVariableUtils> environment;
    private Object searchReply;
    private String creationResponseBody;
    private String failingDelete;
    private boolean rejectHttp2Upgrade;
    private boolean closeSearchResponse;

    @BeforeEach
    void startLoopbackServer() throws Exception {
        environment = fakeEnvironment();
        assertFalse(UserTokenProviderConfig.DEFAULT_INSTANCE.getClientId().isBlank());
        doAnswer(call -> {
            ((UserData) call.getArgument(0)).setAccessToken("cleanup-user");
            return null;
        }).when(adapter).authenticate(any(), anyString());
        when(adapter.getNewS2SToken("aac_manage_case_assignment")).thenReturn("cleanup-service");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort();
        previousFilters = List.copyOf(RestAssured.filters());
        RoleAssignmentCleanup.install();
        RoleAssignmentCleanup.begin(url);
    }

    @AfterEach
    void restoreState() {
        try {
            RoleAssignmentCleanup.finish(adapter);
        } finally {
            server.stop(0);
            RestAssured.replaceFiltersWith(previousFilters);
            environment.close();
        }
        assertNull(serverFailure.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/case-types/test/cases", "/citizens/user/jurisdictions/test/case-types/test/cases",
        "/caseworkers/user/jurisdictions/test/case-types/test/cases/"})
    void cleansCreatorAndAssigneeAfterAnAssertionFails(String path) {
        roles.put(EXISTING, new CopyOnWriteArrayList<>(List.of(tuple(EXISTING, "old", "[CREATOR]"))));
        assertThrows(AssertionError.class, () -> {
            try {
                create(path, FIRST, 201);
                grant(List.of(tuple(FIRST, "assignee", "[CR-1]")), 201);
                assertEquals("expected value", "unexpected value");
            } finally {
                RoleAssignmentCleanup.finish(adapter);
            }
        });
        assertEquals(List.of(tuple(EXISTING, "old", "[CREATOR]")), roles.get(EXISTING));
        assertFalse(roles.containsKey(FIRST));
        assertEquals(2, deletions.size());
        assertEquals(List.of(List.of(FIRST), List.of(FIRST)), searches);
    }

    @ParameterizedTest
    @CsvSource({
        "POST,/case-types/test/cases,400,false", "GET,/case-types/test/cases,201,false",
        "POST,/cases/1234567890123456/events,201,false", "POST,/testing-support/date-case-closed,201,false",
        "POST,/case-types/test/cases,201,true"
    })
    void ignoresFailedRequestsOtherRoutesAndOtherOrigins(String method, String path, int status, boolean otherOrigin) {
        if (otherOrigin) {
            RoleAssignmentCleanup.begin(url.replace("127.0.0.1", "localhost"));
        }
        RestAssured.given().contentType("application/json").body(Map.of("id", FIRST))
            .request(method, url + path + "?status=" + status).then().statusCode(status);
        RoleAssignmentCleanup.finish(adapter);
        assertTrue(searches.isEmpty());
        verifyNoInteractions(adapter);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"case_users\":\"invalid\"}",
        "{\"case_users\":[{\"case_id\":\"1234567890123458\",\"user_id\":\"old\",\"case_role\":\"[CREATOR]\"}]}"})
    void refusesMalformedOrUnownedSearchResults(String response) throws IOException {
        create("/case-types/test/cases", FIRST, 201);
        searchReply = MAPPER.readTree(response);
        assertThrows(FunctionalTestException.class, () -> RoleAssignmentCleanup.finish(adapter));
        assertTrue(deletions.isEmpty());
        assertTrue(roles.containsKey(FIRST));
        assertEquals(List.of(List.of(FIRST)), searches);
    }

    @Test
    void continuesAfterCleanupFailureAndClearsStateBeforeWorkerReuse() {
        create("/case-types/test/cases", FIRST, 201);
        create("/case-types/test/cases", SECOND, 201);
        failingDelete = FIRST;
        FunctionalTestException failure = assertThrows(FunctionalTestException.class,
            () -> RoleAssignmentCleanup.finish(adapter));
        assertTrue(failure.getMessage().contains(FIRST));
        assertFalse(failure.getMessage().contains("cleanup-user"));
        assertTrue(roles.containsKey(FIRST));
        assertFalse(roles.containsKey(SECOND));
        assertEquals(List.of(List.of(FIRST), List.of(FIRST), List.of(FIRST), List.of(SECOND), List.of(SECOND)),
            searches);
        final int previousSearches = searches.size();
        RoleAssignmentCleanup.finish(adapter);
        RoleAssignmentCleanup.begin(url);
        RoleAssignmentCleanup.finish(adapter);
        assertEquals(previousSearches, searches.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 502, 503, 504})
    void retriesTransientSearchFailures(int status) {
        create("/case-types/test/cases", FIRST, 201);
        searchStatuses.add(status);
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(3, searches.size());
        assertEquals(1, deletions.size());
        assertEquals(1, deleteCalls.get());
    }

    @Test
    void retriesTransientDeleteFailuresAfterSearchingAgain() {
        create("/case-types/test/cases", FIRST, 201);
        deleteStatuses.add(503);
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(3, searches.size());
        assertEquals(1, deletions.size());
        assertEquals(2, deleteCalls.get());
    }

    @Test
    void reconcilesADeleteThatCommittedBeforeItsResponseFailed() {
        create("/case-types/test/cases", FIRST, 201);
        committedDeleteStatuses.add(503);
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(2, searches.size());
        assertEquals(1, deletions.size());
        assertEquals(1, deleteCalls.get());
    }

    @Test
    void retriesVerificationFailuresWithoutDeletingAgain() {
        create("/case-types/test/cases", FIRST, 201);
        searchStatuses.addAll(List.of(200, 503));
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(3, searches.size());
        assertEquals(1, deletions.size());
        assertEquals(1, deleteCalls.get());
    }

    @Test
    void retriesWhenTheSearchConnectionClosesWithoutAResponse() {
        create("/case-types/test/cases", FIRST, 201);
        closeSearchResponse = true;
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(3, searches.size());
        assertEquals(1, deletions.size());
        assertEquals(1, deleteCalls.get());
    }

    @Test
    void exhaustsSearchRetriesBeforeContinuingWithTheNextOwnedCase() {
        create("/case-types/test/cases", FIRST, 201);
        create("/case-types/test/cases", SECOND, 201);
        searchStatuses.addAll(List.of(503, 503, 503));
        FunctionalTestException failure = assertThrows(FunctionalTestException.class,
            () -> RoleAssignmentCleanup.finish(adapter));
        assertTrue(failure.getMessage().contains(FIRST));
        assertTrue(roles.containsKey(FIRST));
        assertFalse(roles.containsKey(SECOND));
        assertEquals(List.of(List.of(FIRST), List.of(FIRST), List.of(FIRST), List.of(SECOND), List.of(SECOND)),
            searches);
    }

    @ParameterizedTest
    @CsvSource({"POST,400", "POST,401", "POST,403", "POST,404", "POST,422",
        "DELETE,400", "DELETE,401", "DELETE,403", "DELETE,404", "DELETE,422"})
    void doesNotRetryAuthenticationOrValidationFailures(String method, int status) {
        create("/case-types/test/cases", FIRST, 201);
        ("DELETE".equals(method) ? deleteStatuses : searchStatuses).add(status);
        assertThrows(FunctionalTestException.class, () -> RoleAssignmentCleanup.finish(adapter));
        assertEquals(List.of(List.of(FIRST)), searches);
        assertTrue(deletions.isEmpty());
        assertTrue(roles.containsKey(FIRST));
    }

    @Test
    void emptyScenarioDoesNotAuthenticate() {
        RoleAssignmentCleanup.finish(adapter);
        verifyNoInteractions(adapter);
    }

    @Test
    void cleansAssignmentsWhenGatewayRejectsCleartextHttp2Upgrades() {
        rejectHttp2Upgrade = true;
        create("/case-types/test/cases", FIRST, 201);
        grant(List.of(tuple(FIRST, "assignee", "[CR-1]")), 201);
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(2, deletions.size());
        assertEquals(List.of(List.of(FIRST), List.of(FIRST)), searches);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"id\":null,\"data\":null}", "null", ""})
    void acceptsCreationResponsesWithoutReadAccess(String responseBody) {
        creationResponseBody = responseBody;
        create("/case-types/test/cases", FIRST, 201);
        RoleAssignmentCleanup.finish(adapter);
        assertTrue(searches.isEmpty());
        assertTrue(deletions.isEmpty());
        verifyNoInteractions(adapter);
    }

    @Test
    void stillCleansKnownCasesAfterCreationWithoutReadAccess() {
        create("/case-types/test/cases", FIRST, 201);
        creationResponseBody = "{\"id\":null}";
        create("/case-types/test/cases", SECOND, 201);
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertTrue(roles.containsKey(SECOND));
        assertEquals(List.of(List.of(FIRST), List.of(FIRST)), searches);
    }

    @Test
    void cleansCasesWithNumericReferences() {
        creationResponseBody = "{\"id\":" + FIRST + "}";
        create("/caseworkers/user/jurisdictions/test/case-types/test/cases", FIRST, 201);
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(List.of(List.of(FIRST), List.of(FIRST)), searches);
    }

    @Test
    void rejectsMalformedCreationWithoutLosingEarlierCases() {
        create("/case-types/test/cases", FIRST, 201);
        assertThrows(FunctionalTestException.class, () -> RestAssured.given()
            .contentType("application/json").body(Map.of()).post(url + "/case-types/test/cases"));
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
    }

    @Test
    void forwardsOrganisationOnlyFromSuccessfulAddsForOwnedCases() {
        create("/case-types/test/cases", FIRST, 201);
        roles.put(EXISTING, new CopyOnWriteArrayList<>(List.of(tuple(EXISTING, "old", "[CREATOR]"))));
        grant(List.of(Map.of("case_id", FIRST, "user_id", "assignee", "case_role", "[CR-1]",
                            "organisation_id", "owned-org"),
                      Map.of("case_id", EXISTING, "user_id", "old", "case_role", "[CR-1]",
                            "organisation_id", "other-org")), 201);
        roles.get(FIRST).add(tuple(FIRST, "assignee", "[CR-2]"));
        grant(List.of(Map.of("case_id", FIRST, "user_id", "assignee", "case_role", "[CR-2]",
                            "organisation_id", "failed-org")), 400);
        RoleAssignmentCleanup.finish(adapter);
        assertTrue(deletions.contains(Map.of("case_id", FIRST, "user_id", "assignee", "case_role", "[CR-1]",
                                            "organisation_id", "owned-org")));
        assertTrue(deletions.contains(tuple(FIRST, "assignee", "[CR-2]")));
        assertTrue(deletions.stream().allMatch(role -> FIRST.equals(role.get("case_id"))));
        assertEquals(2, roles.get(EXISTING).size());
        assertEquals(Map.of("owned-org", 1L), organisationDecrements());
    }

    @Test
    void doesNotDecrementOrganisationForAdditionalRoleOnUncountedUser() {
        create("/case-types/test/cases", FIRST, 201);
        grant(List.of(tuple(FIRST, "assignee", "[CR-1]")), 201);
        grant(List.of(organisationTuple(FIRST, "assignee", "[CR-2]", "uncounted-org")), 201);
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(3, deletions.size());
        assertEquals(Map.of(), organisationDecrements());
    }

    @Test
    void countsOrganisationsAcrossTheWholeFirstGrantBatch() {
        create("/case-types/test/cases", FIRST, 201);
        grant(List.of(tuple(FIRST, "assignee", "[CR-1]"),
                      organisationTuple(FIRST, "assignee", "[CR-2]", "first-org"),
                      organisationTuple(FIRST, "assignee", "[CR-2]", "second-org"),
                      organisationTuple(FIRST, "creator", "[CREATOR]", "creator-org")), 201);
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(Map.of("first-org", 1L, "second-org", 1L), organisationDecrements());
        assertTrue(deletions.stream().filter(role -> "[CREATOR]".equals(role.get("case_role")))
            .noneMatch(role -> role.containsKey("organisation_id")));
    }

    @Test
    void carriesCountedOrganisationToSurvivingRoleAfterPartialDelete() {
        create("/case-types/test/cases", FIRST, 201);
        grant(List.of(organisationTuple(FIRST, "assignee", "[CR-1]", "counted-org")), 201);
        grant(List.of(tuple(FIRST, "assignee", "[CR-2]")), 201);
        revoke(List.of(organisationTuple(FIRST, "assignee", "[CR-1]", "counted-org")));
        deletions.clear();
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(Map.of("counted-org", 1L), organisationDecrements());
        assertTrue(deletions.contains(organisationTuple(FIRST, "assignee", "[CR-2]", "counted-org")));
    }

    @Test
    void rebuildsOrganisationCreditAfterFullDeleteAndReAdd() {
        create("/case-types/test/cases", FIRST, 201);
        grant(List.of(organisationTuple(FIRST, "assignee", "[CR-1]", "old-org")), 201);
        revoke(List.of(organisationTuple(FIRST, "assignee", "[CR-1]", "old-org")));
        grant(List.of(organisationTuple(FIRST, "assignee", "[CR-1]", "new-org")), 201);
        deletions.clear();
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(Map.of("new-org", 1L), organisationDecrements());
        assertTrue(deletions.contains(organisationTuple(FIRST, "assignee", "[CR-1]", "new-org")));
    }

    @Test
    void doesNotCountOrganisationAfterRolesWerePutWithoutOrganisation() {
        create("/case-types/test/cases", FIRST, 201);
        putRoles("assignee", List.of("[CREATOR]", "[CR-1]"));
        grant(List.of(organisationTuple(FIRST, "assignee", "[CR-2]", "uncounted-org")), 201);
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(Map.of(), organisationDecrements());
    }

    @Test
    void preservesCountedOrganisationWhenPutReplacesNonCreatorRoles() {
        create("/case-types/test/cases", FIRST, 201);
        grant(List.of(organisationTuple(FIRST, "assignee", "[CR-1]", "counted-org")), 201);
        putRoles("assignee", List.of("[CR-2]"));
        RoleAssignmentCleanup.finish(adapter);
        assertFalse(roles.containsKey(FIRST));
        assertEquals(Map.of("counted-org", 1L), organisationDecrements());
        assertTrue(deletions.contains(organisationTuple(FIRST, "assignee", "[CR-2]", "counted-org")));
    }

    @Test
    void isolatesConcurrentScenarioWorkers() throws Exception {
        CyclicBarrier ready = new CyclicBarrier(2);
        var workers = Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<?>> results = new ArrayList<>();
            for (String reference : List.of(FIRST, SECOND)) {
                results.add(workers.submit(() -> {
                    try (MockedStatic<EnvironmentVariableUtils> workerEnvironment = fakeEnvironment()) {
                        RoleAssignmentCleanup.begin(url);
                        create("/case-types/test/cases", reference, 201);
                        ready.await(10, TimeUnit.SECONDS);
                        RoleAssignmentCleanup.finish(adapter);
                    }
                    return null;
                }));
            }
            for (var result : results) {
                result.get(20, TimeUnit.SECONDS);
            }
            assertTrue(roles.isEmpty());
            assertEquals(4, searches.size());
            assertTrue(searches.stream().allMatch(ids -> ids.size() == 1));
        } finally {
            workers.shutdownNow();
        }
    }

    private static MockedStatic<EnvironmentVariableUtils> fakeEnvironment() {
        MockedStatic<EnvironmentVariableUtils> mock = mockStatic(EnvironmentVariableUtils.class);
        mock.when(() -> EnvironmentVariableUtils.resolvePossibleVariable(anyString())).thenReturn("test-password");
        mock.when(() -> EnvironmentVariableUtils.getRequiredVariable(anyString(), anyString()))
            .thenReturn("test-client");
        return mock;
    }

    private void create(String path, String reference, int status) {
        RestAssured.given().contentType("application/json").body(Map.of("id", reference))
            .post(url + path + "?status=" + status).then().statusCode(status);
    }

    private void grant(List<Map<String, String>> assignments, int status) {
        RestAssured.given().contentType("application/json").body(Map.of("case_users", assignments))
            .post(url + "/case-users?status=" + status).then().statusCode(status);
    }

    private void revoke(List<Map<String, String>> assignments) {
        RestAssured.given().contentType("application/json").header("Authorization", "Bearer cleanup-user")
            .header("ServiceAuthorization", "cleanup-service").body(Map.of("case_users", assignments))
            .delete(url + "/case-users").then().statusCode(200);
    }

    private void putRoles(String user, List<String> caseRoles) {
        RestAssured.given().contentType("application/json").body(Map.of("case_roles", caseRoles))
            .put(url + "/cases/" + FIRST + "/users/" + user).then().statusCode(204);
    }

    private Map<String, Long> organisationDecrements() {
        return deletions.stream().filter(role -> role.containsKey("organisation_id"))
            .collect(Collectors.groupingBy(role -> role.get("organisation_id"), Collectors.counting()));
    }

    private static Map<String, String> organisationTuple(String reference, String user, String role,
                                                        String organisation) {
        Map<String, String> result = new HashMap<>(tuple(reference, user, role));
        result.put("organisation_id", organisation);
        return result;
    }

    private static Map<String, String> tuple(String reference, String user, String role) {
        return Map.of("case_id", reference, "user_id", user, "case_role", role);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (rejectHttp2Upgrade && (exchange.getRequestHeaders().containsKey("Upgrade")
                || exchange.getRequestHeaders().containsKey("HTTP2-Settings"))) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }
            JsonNode body = MAPPER.readTree(exchange.getRequestBody());
            String path = exchange.getRequestURI().getPath();
            int status = "status=400".equals(exchange.getRequestURI().getQuery()) ? 400 : 201;
            Object response = Map.of();
            String rawResponse = null;
            if ("/case-users/search".equals(path) || "DELETE".equals(exchange.getRequestMethod())) {
                assertEquals("Bearer cleanup-user", exchange.getRequestHeaders().getFirst("Authorization"));
                assertEquals("cleanup-service", exchange.getRequestHeaders().getFirst("ServiceAuthorization"));
                status = 200;
            }
            if ("DELETE".equals(exchange.getRequestMethod())) {
                deleteCalls.incrementAndGet();
                Integer failureStatus = deleteStatuses.poll();
                if (failureStatus != null) {
                    exchange.sendResponseHeaders(failureStatus, -1);
                    return;
                }
            }
            if ("/case-users/search".equals(path)) {
                List<String> ids = MAPPER.convertValue(body.path("case_ids"),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
                searches.add(ids);
                if (closeSearchResponse) {
                    closeSearchResponse = false;
                    return;
                }
                Integer failureStatus = searchStatuses.poll();
                if (failureStatus != null && failureStatus != 200) {
                    exchange.sendResponseHeaders(failureStatus, -1);
                    return;
                }
                response = searchReply != null ? searchReply : Map.of("case_users", ids.stream()
                    .flatMap(id -> roles.getOrDefault(id, List.of()).stream()).toList());
            } else if ("/case-users".equals(path) && (status == 200 || status == 201)) {
                for (JsonNode value : body.path("case_users")) {
                    String id = value.path("case_id").asText();
                    Map<String, String> role = tuple(id, value.path("user_id").asText(),
                                                    value.path("case_role").asText());
                    if (status == 200) {
                        deletions.add(MAPPER.convertValue(value,
                            MAPPER.getTypeFactory().constructMapType(Map.class, String.class, String.class)));
                        if (id.equals(failingDelete)) {
                            status = 500;
                            break;
                        }
                        List<Map<String, String>> current = roles.get(id);
                        if (current != null) {
                            current.remove(role);
                        }
                        if (current != null && current.isEmpty()) {
                            roles.remove(id);
                        }
                    } else if (!roles.get(id).contains(role)) {
                        roles.get(id).add(role);
                    }
                }
            } else if ("PUT".equals(exchange.getRequestMethod()) && path.startsWith("/cases/")) {
                String[] parts = path.split("/");
                String id = parts[2];
                String user = parts[4];
                List<Map<String, String>> current = roles.get(id);
                current.removeIf(role -> user.equals(role.get("user_id")));
                for (JsonNode role : body.path("case_roles")) {
                    current.add(tuple(id, user, role.asText()));
                }
                exchange.sendResponseHeaders(204, -1);
                return;
            } else if (body.has("id")) {
                String id = body.get("id").asText();
                roles.put(id, new CopyOnWriteArrayList<>(List.of(tuple(id, "creator", "[CREATOR]"))));
                response = Map.of("id", id);
                rawResponse = creationResponseBody;
            }
            if ("DELETE".equals(exchange.getRequestMethod()) && status == 200) {
                Integer failureStatus = committedDeleteStatuses.poll();
                if (failureStatus != null) {
                    exchange.sendResponseHeaders(failureStatus, -1);
                    return;
                }
            }
            byte[] bytes = rawResponse == null ? MAPPER.writeValueAsBytes(response)
                : rawResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (RuntimeException | AssertionError failure) {
            serverFailure.set(failure);
            exchange.sendResponseHeaders(500, -1);
        } finally {
            exchange.close();
        }
    }
}
