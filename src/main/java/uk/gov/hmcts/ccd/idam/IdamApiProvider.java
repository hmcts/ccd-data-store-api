package uk.gov.hmcts.ccd.idam;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.OAuth2Params;

@Service
public class IdamApiProvider {

    private final IdamApi idamApi;

    public IdamApiProvider(OAuth2Params oAuth2Params) {
        this.idamApi = Feign.builder()
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .target(IdamApi.class, oAuth2Params.getIdamBaseURL());
    }

    public IdamApi provide() {
        return this.idamApi;
    }
}
