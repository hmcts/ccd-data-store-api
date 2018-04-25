package uk.gov.hmcts.ccd.datastore.tests.helper.idam;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class IdamHelper {

    private final Map<String, AuthenticatedUser> users = new HashMap<>();

    private final IdamApi idamApi;

    public IdamHelper(String idamBaseUrl) {
        idamApi = Feign.builder()
                       .encoder(new JacksonEncoder())
                       .decoder(new JacksonDecoder())
                       .target(IdamApi.class, idamBaseUrl);
    }

    public AuthenticatedUser authenticate(String email, String password) {
        return users.computeIfAbsent(email, e -> {
            final String basicAuth = getBasicAuthHeader(email, password);
//            final IdamApi.AuthenticateUserResponse authResponse = idamApi.authenticateUser(basicAuth);
            final IdamApi.AuthenticateUserResponse authResponse = idamApi.testingLease("caseworker-autotest1");
            final IdamApi.IdamUser user = idamApi.getUser(authResponse.getAccessToken());

            return new AuthenticatedUser(user.getId(), email, authResponse.getAccessToken(), user.getRoles());
        });
    }

    private String getBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        return new String(Base64.getEncoder().encode(auth.getBytes()));
    }
}
