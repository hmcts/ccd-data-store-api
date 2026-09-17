package uk.gov.hmcts.ccd.datastore.befta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
import java.util.concurrent.ExecutionException;

/**
 * Creates IDAM test users from JSON files under {@code idamUsers/}, using the IDAM Testing Support API.
 * Uses PUT /test/idam/users/{userId} so existing users are updated (including password) on conflict.
 */
public final class IdamTestingSupportUserCreator {

    private static final String IDAM_USERS_RESOURCE_DIR = "idamUsers";
    private static final String CREATE_OR_UPDATE_USER_PATH = "/test/idam/users/{userId}";
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
        if (userNode == null || userNode.get("id") == null || StringUtils.isBlank(userNode.get("id").asText())) {
            throw new RuntimeException("IDAM user JSON must contain user.id: " + jsonFile.getPath());
        }
        String userId = userNode.get("id").asText();
        String email = userNode.has("email") ? userNode.get("email").asText() : userId;

        Response response = RestAssured
            .given(new RequestSpecBuilder().setBaseUri(idamTsUrl).build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .pathParam("userId", userId)
            .body(requestJson)
            .contentType(ContentType.JSON)
            .when()
            .put(CREATE_OR_UPDATE_USER_PATH);

        if (response.getStatusCode() == HttpStatus.OK.value()) {
            BeftaUtils.defaultLog(
                "IDAM user created or updated from: " + jsonFile.getPath()
                    + " (email=" + email + ", id=" + userId + ")");
        } else {
            BeftaUtils.defaultLog("Error when creating/updating IDAM user from: " + jsonFile.getPath()
                + " (email=" + email + ", id=" + userId + ")");
            String message = "Call to create/update IDAM user failed with response body: "
                + response.body().prettyPrint();
            message += "\nand http code: " + response.statusCode();
            throw new RuntimeException(message);
        }
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
