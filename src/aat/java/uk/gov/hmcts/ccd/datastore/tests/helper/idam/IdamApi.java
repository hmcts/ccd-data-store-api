package uk.gov.hmcts.ccd.datastore.tests.helper.idam;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

public interface IdamApi {

    @RequestLine("POST /oauth2/authorize"
        + "?response_type={response_type}"
        + "&client_id={client_id}"
        + "&redirect_uri={redirect_uri}")
    @Headers("Authorization: {authorization}")
    AuthenticateUserResponse authenticateUser(@Param("authorization") String authorization,
                                              @Param("response_type") String responseType,
                                              @Param("client_id") String clientId,
                                              @Param("redirect_uri") String redirectUri);

    @RequestLine("POST /oauth2/token"
        + "?code={code}"
        + "&grant_type={grant_type}"
        + "&client_id={client_id}"
        + "&client_secret={client_secret}"
        + "&redirect_uri={redirect_uri}")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    TokenExchangeResponse exchangeCode(@Param("code") String code,
                                       @Param("grant_type") String grantType,
                                       @Param("client_id") String clientId,
                                       @Param("client_secret") String clientSecret,
                                       @Param("redirect_uri") String redirectUri);

    @RequestLine("GET /details")
    @Headers("Authorization: Bearer {access_token}")
    IdamUser getUser(@Param("access_token") String accessToken);

    class AuthenticateUserResponse {
        @JsonProperty("code")
        private String code;

        public String getCode() {
            return code;
        }
    }

    class TokenExchangeResponse {

        @JsonProperty("access_token")
        private String accessToken;

        public String getAccessToken() {
            return accessToken;
        }
    }

    class IdamUser {
        @JsonProperty("id")
        private String id;

        @JsonProperty("roles")
        private List<String> roles;

        public String getId() {
            return id;
        }

        public List<String> getRoles() {
            return roles;
        }
    }
}

