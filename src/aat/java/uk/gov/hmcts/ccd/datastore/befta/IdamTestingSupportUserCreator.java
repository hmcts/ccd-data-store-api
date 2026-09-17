package uk.gov.hmcts.ccd.datastore.befta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.auth.UserTokenProviderConfig;
import uk.gov.hmcts.befta.data.UserData;
import uk.gov.hmcts.befta.util.BeftaUtils;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.befta.util.JsonUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Creates IDAM test users from JSON files under {@code idamUsers/}, using the IDAM Testing Support API.
 */
public final class IdamTestingSupportUserCreator {

    private static final String IDAM_USERS_RESOURCE_DIR = "idamUsers";
    private static final String CREATE_USER_PATH = "/test/idam/users";
    private static final String CREATE_OR_UPDATE_USER_PATH = "/test/idam/users/{userId}";
    private static final String GET_USER_BY_EMAIL_PATH = "/test/idam/users";
    private static final String IDAM_USER_PROVISIONER_TD = "features/common/users/BeftaMasterCaseworker.td.json";
    private static final String UNRESOLVED_PLACEHOLDER_PREFIX = "[[$";

    private IdamTestingSupportUserCreator() {
    }

    public static void createUsersInIdam() {
        String idamTsUrl = EnvironmentVariableUtils.getOptionalVariable("IDAM_TESTING_SUPPORT_URL");

        if (StringUtils.isBlank(idamTsUrl)) {
            BeftaUtils.defaultLog("Skipping user creation in IDAM as IDAM_TESTING_SUPPORT_URL is not configured.");
            return;
        }

        File idamUsersDir = BeftaUtils.getFileFromResource(IDAM_USERS_RESOURCE_DIR);
        if (!idamUsersDir.exists() || !idamUsersDir.isDirectory()) {
            BeftaUtils.defaultLog(
                "Skipping user creation in IDAM as " + IDAM_USERS_RESOURCE_DIR + " was not found at "
                    + idamUsersDir.getAbsolutePath());
            return;
        }

        UserData provisioner = getIdamUserProvisioner();

        FileFilter fileFilter = file -> !file.isDirectory() && file.getName().endsWith(".json");
        File[] jsonFiles = idamUsersDir.listFiles(fileFilter);
        if (jsonFiles == null || jsonFiles.length == 0) {
            BeftaUtils.defaultLog("No IDAM user JSON files under " + idamUsersDir.getAbsolutePath());
            return;
        }

        for (File jsonFile : jsonFiles) {
            createOrUpdateUserFromFile(jsonFile, idamTsUrl, provisioner.getAccessToken());
        }
    }

    private static void createOrUpdateUserFromFile(File jsonFile, String idamTsUrl, String accessToken) {
        JsonNode requestJson;

        try {
            requestJson = JsonUtils.readObjectFromJsonFile(jsonFile.getPath(), JsonNode.class);
            resolvePlaceholdersInRequest(requestJson);
        } catch (IOException e) {
            throw new RuntimeException("Error loading user data from: " + jsonFile.getPath(), e);
        }

        JsonNode userNode = requestJson.get("user");
        if (userNode == null || !userNode.has("email")) {
            throw new RuntimeException("IDAM user JSON must contain user.email: " + jsonFile.getPath());
        }
        String email = userNode.get("email").asText();
        String configuredUserId = userNode.has("id") ? userNode.get("id").asText() : null;

        Optional<String> existingUserId = lookupExistingUserIdByEmail(idamTsUrl, accessToken, email);
        boolean userAlreadyExists = existingUserId.isPresent();
        String userId = existingUserId.orElse(configuredUserId);

        if (StringUtils.isBlank(userId)) {
            throw new RuntimeException(
                "IDAM user JSON must contain user.id when email is not yet registered: " + jsonFile.getPath()
            );
        }

        if (configuredUserId != null && !configuredUserId.equals(userId)) {
            BeftaUtils.defaultLog(
                "IDAM user id from email lookup (" + userId + ") differs from JSON id (" + configuredUserId + ")");
            ((ObjectNode) userNode).put("id", userId);
        }

        Response response = userAlreadyExists
            ? putCreateOrUpdateUser(idamTsUrl, accessToken, userId, requestJson)
            : postCreateUser(idamTsUrl, accessToken, requestJson);

        int status = response.getStatusCode();
        if (status == HttpStatus.OK.value() || status == HttpStatus.CREATED.value()) {
            BeftaUtils.defaultLog(
                "IDAM user created or updated from: " + jsonFile.getPath()
                    + " (email=" + email + ", id=" + userId + ", http=" + status + ")");
        } else {
            failCreateOrUpdate(jsonFile, email, userId, response);
        }
    }

    private static Optional<String> lookupExistingUserIdByEmail(
        String idamTsUrl, String accessToken, String email) {
        Response response = authorisedRequest(idamTsUrl, accessToken)
            .queryParam("email", email)
            .when()
            .get(GET_USER_BY_EMAIL_PATH);

        if (response.getStatusCode() == HttpStatus.OK.value()) {
            JsonNode body = response.as(JsonNode.class);
            if (body.has("id") && StringUtils.isNotBlank(body.get("id").asText())) {
                return Optional.of(body.get("id").asText());
            }
        }
        return Optional.empty();
    }

    private static Response postCreateUser(String idamTsUrl, String accessToken, JsonNode requestJson) {
        return authorisedRequest(idamTsUrl, accessToken)
            .body(requestJson)
            .contentType(ContentType.JSON)
            .when()
            .post(CREATE_USER_PATH);
    }

    private static Response putCreateOrUpdateUser(
        String idamTsUrl, String accessToken, String userId, JsonNode requestJson) {
        return authorisedRequest(idamTsUrl, accessToken)
            .pathParam("userId", userId)
            .body(requestJson)
            .contentType(ContentType.JSON)
            .when()
            .put(CREATE_OR_UPDATE_USER_PATH);
    }

    private static RequestSpecification authorisedRequest(String idamTsUrl, String accessToken) {
        return RestAssured
            .given(new RequestSpecBuilder().setBaseUri(idamTsUrl).build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private static void failCreateOrUpdate(File jsonFile, String email, String userId, Response response) {
        BeftaUtils.defaultLog("Error when creating/updating IDAM user from: " + jsonFile.getPath()
            + " (email=" + email + ", id=" + userId + ")");
        String message = "Call to create/update IDAM user failed with response body: "
            + response.body().prettyPrint();
        message += "\nand http code: " + response.statusCode();
        message += "\nCommon causes: IDAM role not registered in AAT (check roleNames in JSON), "
            + "or email already linked to a different user id than user.id in JSON.";
        throw new RuntimeException(message);
    }

    private static void resolvePlaceholdersInRequest(JsonNode requestJson) {
        if (!requestJson.isObject()) {
            return;
        }
        ObjectNode root = (ObjectNode) requestJson;
        if (root.has("password")) {
            root.put("password", resolveRequiredValue(root.get("password").asText(), "password"));
        }
        JsonNode userNode = root.get("user");
        if (userNode != null && userNode.isObject() && userNode.has("email")) {
            ((ObjectNode) userNode).put(
                "email",
                resolveRequiredValue(userNode.get("email").asText(), "user.email")
            );
        }
    }

    private static String resolveRequiredValue(String rawValue, String fieldDescription) {
        String value = EnvironmentVariableUtils.resolvePossibleVariable(rawValue);
        if (StringUtils.isBlank(value) || value.contains(UNRESOLVED_PLACEHOLDER_PREFIX)) {
            throw new RuntimeException(
                "Unresolved environment variable for IDAM user field '" + fieldDescription + "'. "
                    + "Ensure Jenkins Key Vault secrets are mapped (e.g. CCD_DISPOSER_PAYMENT_USER_EMAIL). "
                    + "Raw value: " + rawValue
            );
        }
        return value;
    }

    private static UserData getIdamUserProvisioner() {
        UserData provisioner;

        try {
            JsonNode userJson = JsonUtils.readObjectFromJsonFile(
                BeftaUtils.getFileFromResource(IDAM_USER_PROVISIONER_TD).getPath(),
                JsonNode.class
            );

            provisioner = new UserData(
                resolveRequiredValue(userJson.get("username").asText(), "provisioner.username"),
                resolveRequiredValue(userJson.get("password").asText(), "provisioner.password")
            );
        } catch (IOException e) {
            throw new RuntimeException("Error loading user data for IDAM user provisioning", e);
        }

        try {
            BeftaMain.getAdapter().authenticate(provisioner, UserTokenProviderConfig.DEFAULT_INSTANCE.getClientId());
        } catch (ExecutionException e) {
            throw new RuntimeException("Authenticating IDAM user provisioner failed.", e);
        }

        return provisioner;
    }
}
