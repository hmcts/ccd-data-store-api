package uk.gov.hmcts.ccd.security.idam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.idam.client.OAuth2Configuration;
import uk.gov.hmcts.reform.idam.client.models.TokenRequest;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

@Service
@AutoConfiguration
public class HmctsAccessClient {

    public static final String AUTH_TYPE = "code";
    public static final String GRANT_TYPE = "authorization_code";
    public static final String OPENID_GRANT_TYPE = "password";

    public static final String BASIC_AUTH_TYPE = "Basic";
    public static final String BEARER_AUTH_TYPE = "Bearer";
    public static final String CODE = "code";

    private final HmctsAccessApi hmctsAccessApi;
    private final OAuth2Configuration oauth2Configuration;

    @Autowired
    public HmctsAccessClient(HmctsAccessApi hmctsAccessApi, OAuth2Configuration oauth2Configuration) {
        this.hmctsAccessApi = hmctsAccessApi;
        this.oauth2Configuration = oauth2Configuration;
    }

    // when using the access token you may need to add "Bearer "
    public TokenResponse getAccessTokenResponse(String username, String password) {
        return hmctsAccessApi.generateOpenIdToken(
            new TokenRequest(
                oauth2Configuration.getClientId(),
                oauth2Configuration.getClientSecret(),
                OPENID_GRANT_TYPE,
                oauth2Configuration.getRedirectUri(),
                username,
                password,
                oauth2Configuration.getClientScope(),
                null,
                null
            ));
    }

    public String getAccessToken(String username, String password) {
        return BEARER_AUTH_TYPE + " " + getAccessTokenResponse(username, password).accessToken;
    }

    public UserInfo getUserInfo(String bearerToken) {
        return hmctsAccessApi.retrieveUserInfo(bearerToken);
    }
    
}
