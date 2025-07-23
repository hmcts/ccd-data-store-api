package uk.gov.hmcts.ccd.domain.service.getevents;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class LocalGetEventsOperation implements AuditEventLoader {

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
