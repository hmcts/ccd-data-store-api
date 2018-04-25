package uk.gov.hmcts.ccd.datastore.tests.helper.idam;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

public interface IdamApi {

    @RequestLine("POST /oauth2/authorize")
    @Headers("Authorization: Basic {basic_auth}")
    AuthenticateUserResponse authenticateUser(@Param("basic_auth") String basicAuth);

    @RequestLine("POST /testing-support/lease?id=1&role={role}")
    AuthenticateUserResponse testingLease(@Param("role") String role);

    @RequestLine("GET /details")
    @Headers("Authorization: Bearer {access_token}")
    IdamUser getUser(@Param("access_token") String accessToken);

    class AuthenticateUserResponse {
        @JsonProperty("access-token")
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
