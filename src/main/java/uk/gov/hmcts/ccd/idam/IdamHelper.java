package uk.gov.hmcts.ccd.idam;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.OAuth2Params;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class IdamHelper {

    protected static final String AUTHORIZATION_CODE = "authorization_code";
    protected static final String CODE = "code";
    private static final String BASIC = "Basic ";

    private final Map<String, AuthenticatedUser> users = new HashMap<>();

    private final IdamApi idamApi;
    private OAuth2Params oAuth2Params;

    public IdamHelper(OAuth2Params oAuth2Params, IdamApiProvider idamApiProvider) {
        this.idamApi = idamApiProvider.provide();
        this.oAuth2Params = oAuth2Params;
    }

    public AuthenticatedUser authenticate(String email, String password) {
        return users.computeIfAbsent(email, e -> {
            final String accessToken = getIdamOauth2Token(email, password);
            final IdamApi.IdamUser user = idamApi.getUser(accessToken);

            return new AuthenticatedUser(user.getId(), email, accessToken, user.getRoles());
        });
    }

    public String getIdamOauth2Token(String username, String password) {
        String authorisation = username + ":" + password;
        String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

        IdamApi.AuthenticateUserResponse authenticateUserResponse = idamApi.authenticateUser(
            BASIC + base64Authorisation,
            CODE,
            oAuth2Params.getOauth2ClientId(),
            oAuth2Params.getOauth2RedirectUrl()
        );

        IdamApi.TokenExchangeResponse tokenExchangeResponse = idamApi.exchangeCode(
            authenticateUserResponse.getCode(),
            AUTHORIZATION_CODE,
            oAuth2Params.getOauth2ClientId(),
            oAuth2Params.getOauth2ClientSecret(),
            oAuth2Params.getOauth2RedirectUrl()
        );

        return tokenExchangeResponse.getAccessToken();
    }
}
