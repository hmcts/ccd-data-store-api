package uk.gov.hmcts.ccd.datastore.tests.helper.idam;

import java.util.HashMap;
import java.util.Map;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

public class IdamHelper {

    private static final String GRANT_TYPE = "password";

    private final Map<String, AuthenticatedUser> users = new HashMap<>();

    private final HmctsAccessApi hmctsAccessApi;
    private final OAuth2 oauth2;

    public IdamHelper(String idamBaseUrl, OAuth2 oauth2) {
        hmctsAccessApi = Feign.builder()
                             .encoder(new JacksonEncoder())
                             .decoder(new JacksonDecoder())
                             .target(HmctsAccessApi.class, idamBaseUrl);
        this.oauth2 = oauth2;
    }

    public AuthenticatedUser authenticate(String email, String password) {
        return users.computeIfAbsent(email, e -> {
            final String accessToken = getIdamOauth2Token(email, password);
            final HmctsAccessApi.IdamUser user = hmctsAccessApi.getUser(accessToken);

            return new AuthenticatedUser(user.getUid(), email, accessToken, user.getRoles());
        });
    }

    public String getIdamOauth2Token(String username, String password) {
        HmctsAccessApi.TokenResponse tokenResponse = hmctsAccessApi.generateOpenIdToken(
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
