package uk.gov.hmcts.ccd.datastore.befta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.datastore.tests.Env;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.IdamHelper;
import uk.gov.hmcts.ccd.datastore.tests.helper.idam.OAuth2;
import uk.gov.hmcts.ccd.security.OidcIssuerConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public final class JwtIssuerVerificationApp {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JwtIssuerVerificationApp() {
    }

    public static void main(String[] args) throws Exception {
        Set<String> allowedIssuers = OidcIssuerConfiguration.allowedIssuers(
            Env.require("OIDC_ISSUER"),
            System.getenv("OIDC_ALLOWED_ISSUERS")
        );
        String idamBaseUrl = Env.require("IDAM_API_URL_BASE");
        CredentialValuePair credentials = firstAvailableCredentials(List.of(
            new CredentialVariablePair("CCD_CASEWORKER_AUTOTEST_EMAIL", "CCD_CASEWORKER_AUTOTEST_PASSWORD"),
            new CredentialVariablePair("DEFINITION_IMPORTER_USERNAME", "DEFINITION_IMPORTER_PASSWORD")
        ));

        IdamHelper idamHelper = new IdamHelper(idamBaseUrl, OAuth2.INSTANCE);
        String accessToken = idamHelper.getIdamOauth2Token(credentials.username(), credentials.password());
        String actualIssuer = decodeIssuer(accessToken);

        if (!allowedIssuers.contains(actualIssuer)) {
            throw new IllegalStateException(
                "OIDC issuer mismatch: expected one of `" + String.join("`, `", allowedIssuers)
                    + "` but token iss was `" + actualIssuer + "`"
            );
        }

        log.info("Verified functional test token iss is allowed: {}", actualIssuer);
    }

    private static CredentialValuePair firstAvailableCredentials(
        List<CredentialVariablePair> credentialVariablePairs
    ) {
        for (CredentialVariablePair credentialVariablePair : credentialVariablePairs) {
            String username = System.getenv(credentialVariablePair.usernameVariable());
            String password = System.getenv(credentialVariablePair.passwordVariable());
            if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
                return new CredentialValuePair(username, password);
            }
        }

        String expectedVariables = credentialVariablePairs.stream()
            .map(pair -> pair.usernameVariable() + "/" + pair.passwordVariable())
            .collect(Collectors.joining(", "));

        throw new IllegalStateException(
            "No credentials available for JWT issuer verification. "
                + "Expected one of: " + expectedVariables
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

    private record CredentialVariablePair(String usernameVariable, String passwordVariable) {
    }

    private record CredentialValuePair(String username, String password) {
    }
}
