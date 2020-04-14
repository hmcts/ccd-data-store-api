package uk.gov.hmcts.ccd.auditlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AuditRepository.class);

    public void save(final AuditEntry auditEntry) {
        LOG.info(auditEntry.toString());
    }
}
