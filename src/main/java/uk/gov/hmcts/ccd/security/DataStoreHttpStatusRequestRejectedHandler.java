package uk.gov.hmcts.ccd.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import uk.gov.hmcts.ccd.appinsights.AppInsights;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Class for handling RequestRejectedExceptions raised by Spring StrictHttpFirewall.
 * Based on the Spring HttpStatusRequestRejectedHandler class.
 */
public class DataStoreHttpStatusRequestRejectedHandler implements RequestRejectedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DataStoreHttpStatusRequestRejectedHandler.class);

    private final AppInsights appInsights;

    private final int httpError;

    public DataStoreHttpStatusRequestRejectedHandler(AppInsights appInsights) {
        this(appInsights, HttpServletResponse.SC_FORBIDDEN);
    }

    public DataStoreHttpStatusRequestRejectedHandler(AppInsights appInsights, int httpError) {
        this.appInsights = appInsights;
        this.httpError = httpError;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       RequestRejectedException requestRejectedException) throws IOException {
        final String errorMsg = requestRejectedException.getMessage();
        LOG.warn(errorMsg);
        appInsights.trackException(requestRejectedException);
        response.sendError(httpError, errorMsg);
    }
}
