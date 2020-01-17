package uk.gov.hmcts.ccd.datastore.tests.helper;

import feign.Feign;
import feign.jackson.JacksonEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.ServiceAuthTokenGenerator;

public class S2SHelper {

    private final ServiceAuthTokenGenerator tokenGenerator;

    public S2SHelper(final String s2sUrl, final String secret, final String microservice) {
        final ServiceAuthorisationApi serviceAuthorisationApi = Feign.builder()
            .encoder(new JacksonEncoder())
            .contract(new SpringMvcContract())
            .target(ServiceAuthorisationApi.class, s2sUrl);

        this.tokenGenerator = new ServiceAuthTokenGenerator(secret, microservice, serviceAuthorisationApi);
    }

    public String getToken() {
        return tokenGenerator.generate();
    }
}
