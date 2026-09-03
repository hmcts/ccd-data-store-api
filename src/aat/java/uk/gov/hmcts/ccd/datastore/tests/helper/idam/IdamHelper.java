package uk.gov.hmcts.ccd.datastore.tests.helper.idam;

import java.util.HashMap;
import java.util.Map;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

public class IdamHelper {

    private static final String GRANT_TYPE = "password";

    private final Map<String, AuthenticatedUser> users = new HashMap<>();

    private final OidcApi oidcApi;
    private final OAuth2 oauth2;

    public IdamHelper(String idamBaseUrl, OAuth2 oauth2) {
        oidcApi = Feign.builder()
                             .encoder(new JacksonEncoder())
                             .decoder(new JacksonDecoder())
                             .target(OidcApi.class, idamBaseUrl);
        this.oauth2 = oauth2;
    }

    public AuthenticatedUser authenticate(String email, String password) {
        return users.computeIfAbsent(email, e -> {
            final String accessToken = getIdamOauth2Token(email, password);
            final OidcApi.IdamUser user = oidcApi.getUser(accessToken);

            return new AuthenticatedUser(user.getUid(), email, accessToken, user.getRoles());
        });
    }

    public String getIdamOauth2Token(String username, String password) {
        OidcApi.TokenResponse tokenResponse = oidcApi.generateOpenIdToken(
            GRANT_TYPE,
            oauth2.getClientId(),
            oauth2.getClientSecret(),
            oauth2.getRedirectUri(),
            oauth2.getScope(),
            username,
            password
        );

        return tokenResponse.getAccessToken();
    }
}
