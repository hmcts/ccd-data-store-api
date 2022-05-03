package uk.gov.hmcts.ccd.wiremock.extensions;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;

/*
 * Replaces response body with the OAuth JWK Set, i.e. the public keys used to sign the mock OAAuth token
 */
@Slf4j
public class CustomisedResponseTransformer extends ResponseDefinitionTransformer {

    @Override
    public String getName() {
        return "keep-alive-disabler";
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition,
                                        FileSource files, Parameters parameters) {
        return ResponseDefinitionBuilder.like(responseDefinition)
            .withHeader(HttpHeaders.CONNECTION, "close")
            .build();
    }
}
