package uk.gov.hmcts.ccd.domain.service.getevents;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

@Service
@RequiredArgsConstructor
public class LocalAuditEventLoader implements AuditEventLoader {

    private final CaseAuditEventRepository auditEventRepository;

    @Override
    public List<AuditEvent> getEvents(CaseDetails caseDetails) {
        return auditEventRepository.findByCase(caseDetails);
    }

    @Override
    public Optional<AuditEvent> getEvent(CaseDetails caseDetails, Long eventId) {
        return auditEventRepository.findByEventId(eventId);
    }
}
