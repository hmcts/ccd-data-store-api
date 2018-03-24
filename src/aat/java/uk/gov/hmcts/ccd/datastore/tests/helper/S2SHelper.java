package uk.gov.hmcts.ccd.datastore.tests.helper;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import uk.gov.hmcts.auth.provider.service.token.HttpComponentsBasedServiceTokenGenerator;
import uk.gov.hmcts.auth.totp.GoogleTotpAuthenticator;

import java.util.HashMap;

public class S2SHelper {

    private final String baseUrl;
    private final HashMap<String, String> services = new HashMap<>();
    private final GoogleTotpAuthenticator authenticator = new GoogleTotpAuthenticator();
    private final HttpClient httpClient = HttpClients.createDefault();

    public S2SHelper(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken(String microservice, String secret) {
        return services.computeIfAbsent(microservice, e -> {
            final HttpComponentsBasedServiceTokenGenerator tokenGenerator = new HttpComponentsBasedServiceTokenGenerator(
                httpClient,
                baseUrl,
                microservice,
                authenticator,
                secret);
            return tokenGenerator.generate();
        });
    }
}
