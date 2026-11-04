package uk.gov.hmcts.ccd.datastore.tests.helper.idam;

import uk.gov.hmcts.ccd.datastore.tests.Env;

public enum OAuth2 {

    INSTANCE;

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;

    OAuth2() {
        clientId = Env.require("CCD_API_GATEWAY_OAUTH2_CLIENT_ID");
        clientSecret = Env.require("CCD_API_GATEWAY_OAUTH2_CLIENT_SECRET");
        redirectUri = Env.require("CCD_API_GATEWAY_OAUTH2_REDIRECT_URL");
        scope = Env.require("CCD_API_GATEWAY_OAUTH2_SCOPE");
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getScope() {
        return scope;
    }
}
