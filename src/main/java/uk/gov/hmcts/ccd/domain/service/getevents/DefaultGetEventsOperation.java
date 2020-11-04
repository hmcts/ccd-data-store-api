package uk.gov.hmcts.ccd.domain.service.getevents;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
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
@Qualifier("default")
public class DefaultGetEventsOperation implements GetEventsOperation {

    private final CaseAuditEventRepository auditEventRepository;
    private final GetCaseOperation getCaseOperation;
    private final UIDService uidService;
    private static final String RESOURCE_NOT_FOUND //
        = "No case found ( jurisdiction = '%s', case type id = '%s', case reference = '%s' )";
    private static final String CASE_RESOURCE_NOT_FOUND //
        = "No case found ( case reference = '%s' )";
    private static final String CASE_EVENT_NOT_FOUND = "Case audit events not found";

    @Autowired
    public DefaultGetEventsOperation(CaseAuditEventRepository auditEventRepository, @Qualifier(
        CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation, UIDService uidService) {
        this.auditEventRepository = auditEventRepository;
        this.getCaseOperation = getCaseOperation;
        this.uidService = uidService;
    }

    @Override
    public List<AuditEvent> getEvents(CaseDetails caseDetails) {
        return auditEventRepository.findByCase(caseDetails);
    }

    @Override
    public List<AuditEvent> getEvents(String jurisdiction, String caseTypeId, String caseReference) {
        return getEvents(caseReference, () ->
            String.format(RESOURCE_NOT_FOUND, jurisdiction, caseTypeId, caseReference));
    }

    private List<AuditEvent> getEvents(String caseReference, Supplier<String> errorMessageSupplier) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference " + caseReference + " is not valid");
        }

        final CaseDetails caseDetails =
            getCaseOperation.execute(caseReference)
                .orElseThrow(() -> new ResourceNotFoundException(errorMessageSupplier.get()));
        return getEvents(caseDetails);
    }

    @Override
    public List<AuditEvent> getEvents(String caseReference) {
        return getEvents(caseReference, () -> String.format(CASE_RESOURCE_NOT_FOUND, caseReference));
    }

    @Override
    public Optional<AuditEvent> getEvent(String jurisdiction, String caseTypeId, Long eventId) {
        return auditEventRepository.findByEventId(eventId).map(Optional::of)
            .orElseThrow(() -> new ResourceNotFoundException(CASE_EVENT_NOT_FOUND));
    }
}
