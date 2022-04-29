package uk.gov.hmcts.ccd.wiremock.extensions;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import org.springframework.http.HttpStatus;

/*
 * Customises the static stubbed response before sending it back to the client
 */
public abstract class AbstractDynamicResponseTransformer extends ResponseTransformer {

    @Override
    public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
        try {
            return Response.Builder.like(response)
                .but()
                .body(dynamicResponse(request, response, parameters))
                .build();

        } catch (SecurityException ex) {
            return Response.Builder.like(response)
                .but()
                .status(HttpStatus.UNAUTHORIZED.value())
                .statusMessage(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .build();
        }
    }

    @Override
    public boolean applyGlobally() {
        // This flag will ensure this transformer is used only for those request mappings that have the transformer
        // configured
        return false;
    }

    protected abstract String dynamicResponse(Request request, Response response, Parameters parameters);
}
