package uk.gov.hmcts.ccd.security;

import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;

import java.io.IOException;
import java.net.URL;

/**
 * A {@link DefaultResourceRetriever} that reports whether IDAM was actually reached.
 *
 * <p>Nimbus listeners sit above {@code OutageTolerantJWKSetSource}, so they cannot distinguish
 * freshly retrieved keys from stale keys served during an outage. This retriever provides that distinction.
 */
class ObservedResourceRetriever extends DefaultResourceRetriever {

    private final JwkSourceTelemetry telemetry;

    ObservedResourceRetriever(int connectTimeout, int readTimeout, int sizeLimit, JwkSourceTelemetry telemetry) {
        super(connectTimeout, readTimeout, sizeLimit);
        this.telemetry = telemetry;
    }

    @Override
    public Resource retrieveResource(URL url) throws IOException {
        Resource resource = super.retrieveResource(url);
        telemetry.retrievalSucceeded();
        return resource;
    }
}
