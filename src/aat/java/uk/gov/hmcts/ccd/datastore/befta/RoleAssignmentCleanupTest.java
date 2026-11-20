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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
    private final AtomicReference<Throwable> serverFailure = new AtomicReference<>();
    private final TestAutomationAdapter adapter = mock(TestAutomationAdapter.class);
    private HttpServer server;
    private String url;
    private List<Filter> previousFilters;
    private MockedStatic<EnvironmentVariableUtils> environment;
    private Object searchReply;
    private String creationResponseBody;
    private String failingDelete;

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
        final int previousSearches = searches.size();
        RoleAssignmentCleanup.finish(adapter);
        RoleAssignmentCleanup.begin(url);
        RoleAssignmentCleanup.finish(adapter);
        assertEquals(previousSearches, searches.size());
    }

    @Test
    void emptyScenarioDoesNotAuthenticate() {
        RoleAssignmentCleanup.finish(adapter);
        verifyNoInteractions(adapter);
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

    private static Map<String, String> tuple(String reference, String user, String role) {
        return Map.of("case_id", reference, "user_id", user, "case_role", role);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
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
            if ("/case-users/search".equals(path)) {
                List<String> ids = MAPPER.convertValue(body.path("case_ids"),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
                searches.add(ids);
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
                        roles.get(id).remove(role);
                        if (roles.get(id).isEmpty()) {
                            roles.remove(id);
                        }
                    } else {
                        roles.get(id).add(role);
                    }
                }
            } else if (body.has("id")) {
                String id = body.get("id").asText();
                roles.put(id, new CopyOnWriteArrayList<>(List.of(tuple(id, "creator", "[CREATOR]"))));
                response = Map.of("id", id);
                rawResponse = creationResponseBody;
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
