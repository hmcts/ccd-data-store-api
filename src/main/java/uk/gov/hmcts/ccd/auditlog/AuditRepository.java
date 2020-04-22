package uk.gov.hmcts.ccd.auditlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuditRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AuditRepository.class);

    private AuditLogFormatter logFormatter;

    @Autowired
    public AuditRepository(AuditLogFormatter auditLogFormatter) {
        this.logFormatter = auditLogFormatter;
    }

    public void save(final AuditEntry auditEntry) {
        LOG.info(logFormatter.format(auditEntry));
    }
}
