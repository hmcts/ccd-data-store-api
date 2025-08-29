package uk.gov.hmcts.ccd.domain.service.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.ccd.config.JacksonUtils;

public class JcLogger {

    private static final Logger LOG = LoggerFactory.getLogger(JcLogger.class);
    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;

    private final String className;
    private final boolean logEnabled;

    public JcLogger(final String className, final boolean logEnabled) {
        this.className = className;
        this.logEnabled = logEnabled;
    }

    /**
     * Log message.
     */
    public void jclog(String message) {
        if (logEnabled) {
            LOG.info("| JCDEBUG: Info: {}: {}", className, message);
            // LOG.warn("| JCDEBUG: Warn: {}: {}", className, message);
            // LOG.error("| JCDEBUG: Error: {}: {}", className, message);
            // LOG.debug("| JCDEBUG: Debug: {}: {}", className, message);
        }
    }

    /**
     * Get call stack as String (tab separated).
     */
    public String getCallStackAsString() {
        final StringBuilder sb = new StringBuilder();
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Skip the first two elements to exclude getStackTrace() and getCallStackAsString()
        for (int i = 3; i < stackTrace.length; i++) {
            sb.append(stackTrace[i].toString()).append("\t");
        }
        return sb.toString();
    }

    /**
     * Print Object to String (in JSON format).
     */
    public String printObjectToString(final Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            return "ERROR_WRITING_OBJECT";
        }
    }

}
