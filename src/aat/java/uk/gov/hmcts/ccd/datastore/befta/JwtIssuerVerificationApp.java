package uk.gov.hmcts.ccd.datastore.befta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.ccd.datastore.tests.Env;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.IdamHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.OAuth2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JwtIssuerVerificationApp {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JwtIssuerVerificationApp() {
    }

    public static void main(String[] args) throws Exception {
        String expectedIssuer = Env.require("OIDC_ISSUER");
        String idamBaseUrl = Env.require("IDAM_API_URL_BASE");
        String[] credentials = firstAvailableCredentials(
            "CCD_CASEWORKER_AUTOTEST_EMAIL", "CCD_CASEWORKER_AUTOTEST_PASSWORD",
            "DEFINITION_IMPORTER_USERNAME", "DEFINITION_IMPORTER_PASSWORD"
        );

        IdamHelper idamHelper = new IdamHelper(idamBaseUrl, OAuth2.INSTANCE);
        String accessToken = idamHelper.getIdamOauth2Token(credentials[0], credentials[1]);
        String actualIssuer = decodeIssuer(accessToken);

        if (!expectedIssuer.equals(actualIssuer)) {
            throw new IllegalStateException(
                "OIDC_ISSUER mismatch: expected `" + expectedIssuer + "` but token iss was `" + actualIssuer + "`"
            );
        }

        System.out.println("Verified OIDC_ISSUER matches functional test token iss: " + actualIssuer);
    }

    private static String[] firstAvailableCredentials(String... envNames) {
        for (int i = 0; i < envNames.length; i += 2) {
            String username = System.getenv(envNames[i]);
            String password = System.getenv(envNames[i + 1]);
            if (username != null && password != null) {
                return new String[]{username, password};
            }
        }

        throw new IllegalStateException(
            "No credentials available for JWT issuer verification. "
                + "Expected one of: CCD_CASEWORKER_AUTOTEST_EMAIL/PASSWORD or "
                + "DEFINITION_IMPORTER_USERNAME/PASSWORD"
        );
    }

    private static String decodeIssuer(String accessToken) throws Exception {
        String[] parts = accessToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalStateException("Access token is not a JWT");
        }

        byte[] decodedPayload = Base64.getUrlDecoder().decode(padBase64(parts[1]));
        JsonNode payload = OBJECT_MAPPER.readTree(new String(decodedPayload, StandardCharsets.UTF_8));
        JsonNode issuer = payload.get("iss");
        if (issuer == null || issuer.isNull()) {
            throw new IllegalStateException("Access token does not contain an iss claim");
        }
        return issuer.asText();
    }

    private static String padBase64(String value) {
        int remainder = value.length() % 4;
        return remainder == 0 ? value : value + "=".repeat(4 - remainder);
    }
}
