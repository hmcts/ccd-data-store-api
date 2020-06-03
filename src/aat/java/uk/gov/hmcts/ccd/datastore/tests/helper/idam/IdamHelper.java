package uk.gov.hmcts.ccd.datastore.tests.helper.idam;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

public class IdamHelper {

    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String CODE = "code";
    private static final String BASIC = "Basic ";

    private final Map<String, AuthenticatedUser> users = new HashMap<>();

    private final IdamApi idamApi;
    private final OAuth2 oauth2;

    public IdamHelper(String idamBaseUrl, OAuth2 oauth2) {
        idamApi = Feign.builder()
                       .encoder(new JacksonEncoder())
                       .decoder(new JacksonDecoder())
                       .target(IdamApi.class, idamBaseUrl);
        this.oauth2 = oauth2;
    }

    public AuthenticatedUser authenticate(String email, String password) {
        return users.computeIfAbsent(email, e -> {
            final String accessToken = getIdamOauth2Token(email, password);
            final IdamApi.IdamUser user = idamApi.getUser(accessToken);

            return new AuthenticatedUser(user.getUid(), email, accessToken, user.getRoles());
        });
    }

    public String getIdamOauth2Token(String username, String password) {
        String authorisation = username + ":" + password;
        String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

        IdamApi.AuthenticateUserResponse authenticateUserResponse = idamApi.authenticateUser(
            BASIC + base64Authorisation,
            CODE,
            oauth2.getClientId(),
            oauth2.getRedirectUri()
        );

        IdamApi.TokenExchangeResponse tokenExchangeResponse = idamApi.exchangeCode(
            authenticateUserResponse.getCode(),
            AUTHORIZATION_CODE,
            oauth2.getClientId(),
            oauth2.getClientSecret(),
            oauth2.getRedirectUri()
        );

        return tokenExchangeResponse.getAccessToken();
    }
}
