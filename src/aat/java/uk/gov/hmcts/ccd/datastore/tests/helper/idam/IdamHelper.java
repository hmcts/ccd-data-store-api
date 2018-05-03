package uk.gov.hmcts.ccd.datastore.tests.helper.idam;

import feign.Feign;
import feign.Response;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class IdamHelper {

    private final Map<String, AuthenticatedUser> users = new HashMap<>();
    private final Decoder.Default defaultDecoder = new Decoder.Default();

    private final IdamApi idamApi;

    public IdamHelper(String idamBaseUrl) {
        idamApi = Feign.builder()
                       .encoder(new JacksonEncoder())
                       .decoder(new JacksonDecoder())
                       .target(IdamApi.class, idamBaseUrl);
    }

    // FIXME This requires a whitelisted redirect URL
    public AuthenticatedUser authenticate(String email, String password) {
        return users.computeIfAbsent(email, e -> {
            final String basicAuth = getBasicAuthHeader(email, password);
            final IdamApi.AuthenticateUserResponse authResponse = idamApi.authenticateUser(basicAuth);
            final IdamApi.IdamUser user = idamApi.getUser(authResponse.getAccessToken());

            return new AuthenticatedUser(user.getId(), email, authResponse.getAccessToken(), user.getRoles());
        });
    }

    // Relies on IDAM test endpoints. Limitations: not available in prod, only support a single role
    public AuthenticatedUser authenticate(String role) {
        return users.computeIfAbsent(role, e -> {
            try {
                final Response tokenResponse = idamApi.testingLease(role);
                final String accessToken = (String) defaultDecoder.decode(tokenResponse, String.class);
                final IdamApi.IdamUser user = idamApi.getUser(accessToken);
                return new AuthenticatedUser(user.getId(), "", accessToken, user.getRoles());
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private String getBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        return new String(Base64.getEncoder().encode(auth.getBytes()));
    }
}
