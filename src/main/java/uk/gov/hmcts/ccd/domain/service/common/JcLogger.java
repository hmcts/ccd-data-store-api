package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackRequest;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.endpoint.std.TestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

public class JcLogger {

    private static final Logger LOG = LoggerFactory.getLogger(JcLogger.class);

    private final String classname;

    private final boolean enabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JcLogger(final String classname, final boolean enabled) {
        this.classname = classname;
        this.enabled = enabled;
        // Enables serialisation of java.util.Optional and java.time.LocalDateTime
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
    }

    public void jclog(String message) {
        if (enabled) {
            LOG.info("| JCDEBUG: {}: {}", classname, message);
            TestController.jcLog("| JCDEBUG: " + classname + ": " + message);
        }
    }

    public void jclog(String message, int i) {
        jclog(message + ": " + i);
    }

    public void jclog(String message, CaseDataContent caseDataContent) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(caseDataContent));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR: " + e.getMessage());
        }
    }

    public void jclog(String message, CallbackRequest callbackRequest) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(callbackRequest));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR: " + e.getMessage());
        }
    }

    public void jclog(String message, Optional optional) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(optional));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR: " + e.getMessage());
        }
    }

    public void jclog(String message, CaseDetails caseDetails) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(caseDetails));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR: " + e.getMessage());
        }
    }

    public void jclog(String message, SecurityClassification securityClassification) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(securityClassification));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR: " + e.getMessage());
        }
    }

    public void jclog(String message, StartEventResult startEventResult) {
        try {
            jclog(message + ": " + objectMapper.writeValueAsString(startEventResult));
        } catch (JsonProcessingException e) {
            jclog(message + ": JSON ERROR: " + e.getMessage());
        }
    }

    public String getObjectAsString(final Map<String, JsonNode> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            jclog("JSON ERROR: " + e.getMessage());
            return "JSON ERROR: " + e.getMessage();
        }
    }

    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString().replaceAll("\r\n", " ").replaceAll("\n", " ");
        return stackTrace.hashCode() + " " + stackTrace;
    }
}
