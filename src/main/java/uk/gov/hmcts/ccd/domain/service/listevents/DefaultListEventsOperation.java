package uk.gov.hmcts.ccd.domain.service.listevents;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.util.List;

@Service
@Qualifier("default")
public class DefaultListEventsOperation implements ListEventsOperation {

    private final CaseAuditEventRepository auditEventRepository;

    @Autowired
    public DefaultListEventsOperation(CaseAuditEventRepository auditEventRepository) {

        this.auditEventRepository = auditEventRepository;
    }

    @Override
    public List<AuditEvent> execute(CaseDetails caseDetails) {
        return auditEventRepository.findByCase(caseDetails);
    }
}
