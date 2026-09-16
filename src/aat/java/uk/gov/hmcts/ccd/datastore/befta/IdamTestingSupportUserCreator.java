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
 * See am-org-role-mapping-service {@code OrmTestDataLoader} for the original pattern.
 */
public final class IdamTestingSupportUserCreator {

    private static final String IDAM_USERS_RESOURCE_DIR = "idamUsers";
    private static final String CREATE_USER_PATH = "/test/idam/users";
    private static final String IDAM_USER_PROVISIONER_TD = "features/common/users/BeftaMasterCaseworker.td.json";

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
            BeftaUtils.defaultLog("Skipping user creation in IDAM as " + IDAM_USERS_RESOURCE_DIR + " was not found.");
            return;
        }

        UserData provisioner = getIdamUserProvisioner();

        FileFilter fileFilter = file -> !file.isDirectory() && file.getName().endsWith(".json");
        File[] jsonFiles = idamUsersDir.listFiles(fileFilter);
        if (jsonFiles == null) {
            return;
        }

        for (File jsonFile : jsonFiles) {
            createUserFromFile(jsonFile, idamTsUrl, provisioner.getAccessToken());
        }
    }

    private static void createUserFromFile(File jsonFile, String idamTsUrl, String accessToken) {
        JsonNode requestJson;

        try {
            requestJson = JsonUtils.readObjectFromJsonFile(jsonFile.getPath(), JsonNode.class);
            updateNodeValueFromEnvironmentVariable(requestJson, "password");
            JsonNode userNode = requestJson.findValue("user");
            if (userNode != null) {
                updateNodeValueFromEnvironmentVariable(userNode, "email");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading user data from: " + jsonFile.getPath(), e);
        }

        Response response = RestAssured
            .given(new RequestSpecBuilder().setBaseUri(idamTsUrl).build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .body(requestJson)
            .contentType(ContentType.JSON)
            .when()
            .post(CREATE_USER_PATH);

        if (response.getStatusCode() == HttpStatus.CREATED.value()) {
            BeftaUtils.defaultLog("IDAM user created from: " + jsonFile.getPath());
        } else if (response.getStatusCode() == HttpStatus.CONFLICT.value()) {
            BeftaUtils.defaultLog("IDAM user already exists for: " + jsonFile.getPath());
        } else {
            BeftaUtils.defaultLog("Error when creating IDAM user from: " + jsonFile.getPath());
            String message = "Call to create IDAM user failed with response body: " + response.body().prettyPrint();
            message += "\nand http code: " + response.statusCode();
            throw new RuntimeException(message);
        }
    }

    private static UserData getIdamUserProvisioner() {
        UserData provisioner;

        try {
            JsonNode userJson = JsonUtils.readObjectFromJsonFile(
                BeftaUtils.getFileFromResource(IDAM_USER_PROVISIONER_TD).getPath(),
                JsonNode.class
            );

            provisioner = new UserData(
                EnvironmentVariableUtils.resolvePossibleVariable(userJson.findValue("username").asText()),
                EnvironmentVariableUtils.resolvePossibleVariable(userJson.findValue("password").asText())
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

    private static void updateNodeValueFromEnvironmentVariable(JsonNode parentNode, String fieldName) {
        JsonNode fieldNode = parentNode.findValue(fieldName);
        if (fieldNode == null || !parentNode.isObject()) {
            return;
        }
        String value = EnvironmentVariableUtils.resolvePossibleVariable(fieldNode.asText());
        ((ObjectNode) parentNode).put(fieldName, value);
    }
}
