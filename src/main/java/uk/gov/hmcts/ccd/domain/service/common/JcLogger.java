package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import java.util.Optional;

public class JcLogger {

    private static final Logger LOG = LoggerFactory.getLogger(JcLogger.class);

    private final String classname;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JcLogger(final String classname) {
        this.classname = classname;
        // Enables serialisation of java.util.Optional and java.time.LocalDateTime
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
    }

    public void jclog(String message) {
        LOG.info("| JCDEBUG: {}: {}", classname, message);
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
}
