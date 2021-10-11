package uk.gov.hmcts.ccd.domain.service.getevents;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;

import static java.util.Collections.singletonList;

@Service
@Qualifier("classified")
public class ClassifiedGetEventsOperation implements GetEventsOperation {

    private final GetEventsOperation getEventsOperation;
    private final SecurityClassificationServiceImpl classificationService;
    private final GetCaseOperation getCaseOperation;

    public ClassifiedGetEventsOperation(@Qualifier("default") GetEventsOperation getEventsOperation,
                                        SecurityClassificationServiceImpl classificationService,
                                        @Qualifier(CreatorGetCaseOperation.QUALIFIER)
                                        final GetCaseOperation getCaseOperation) {

        this.getEventsOperation = getEventsOperation;
        this.classificationService = classificationService;
        this.getCaseOperation = getCaseOperation;
    }

    @Override
    public List<AuditEvent> getEvents(CaseDetails caseDetails) {
        final List<AuditEvent> events = getEventsOperation.getEvents(caseDetails);

        return secureEvents(events, caseDetails);
    }

    @Override
    public List<AuditEvent> getEvents(String jurisdiction, String caseTypeId, String caseReference) {
        return secureEvents(getEventsOperation.getEvents(jurisdiction, caseTypeId, caseReference), caseReference);
    }

    @Override
    public List<AuditEvent> getEvents(String caseReference) {
        return secureEvents(getEventsOperation.getEvents(caseReference), caseReference);
    }

    @Override
    public Optional<AuditEvent> getEvent(CaseDetails caseDetails, String caseTypeId, Long eventId) {
        return getEventsOperation.getEvent(caseDetails, caseTypeId, eventId).flatMap(
            event -> secureEvents(singletonList(event), caseDetails).stream().findFirst());
    }

    private List<AuditEvent> secureEvents(List<AuditEvent> events, String caseReference) {
        CaseDetails caseDetails = getCaseOperation.execute(caseReference).orElse(null);
        return secureEvents(events, caseDetails);
    }

    private List<AuditEvent> secureEvents(List<AuditEvent> events, CaseDetails caseDetails) {
        if (null == caseDetails) {
            return Lists.newArrayList();
        }
        return classificationService.applyClassification(caseDetails, events);
    }
}
