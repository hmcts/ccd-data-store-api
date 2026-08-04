package uk.gov.hmcts.ccd.datastore.tests.helper.idam;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

public interface IdamApi {

    @RequestLine("POST /o/token")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @Body("grant_type={grant_type}&client_id={client_id}&client_secret={client_secret}"
        + "&redirect_uri={redirect_uri}&scope={scope}&username={username}&password={password}")
    TokenResponse generateOpenIdToken(@Param("grant_type") String grantType,
                                       @Param("client_id") String clientId,
                                       @Param("client_secret") String clientSecret,
                                       @Param("redirect_uri") String redirectUri,
                                       @Param("scope") String scope,
                                       @Param("username") String username,
                                       @Param("password") String password);

    @RequestLine("GET /o/userinfo")
    @Headers("Authorization: Bearer {access_token}")
    IdamUser getUser(@Param("access_token") String accessToken);

    class AuthenticateUserResponse {
        @JsonProperty("code")
        private String code;

        public String getCode() {
            return code;
        }
    }

    class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_in")
        private String expiresIn;

        @JsonProperty("id_token")
        private String idToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("scope")
        private String scope;

        @JsonProperty("token_type")
        private String tokenType;

        public String getAccessToken() {
            return accessToken;
        }

        public String getExpiresIn() {
            return expiresIn;
        }

        public String getIdToken() {
            return idToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public String getScope() {
            return scope;
        }

        public String getTokenType() {
            return tokenType;
        }
    }

    class IdamUser {
        @JsonProperty("uid")
        private String uid;

        @JsonProperty("roles")
        private List<String> roles;

        public String getUid() {
            return uid;
        }

        public List<String> getRoles() {
            return roles;
        }
    }
}

