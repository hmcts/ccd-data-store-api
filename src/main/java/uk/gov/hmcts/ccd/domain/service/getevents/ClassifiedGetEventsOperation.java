package uk.gov.hmcts.ccd.domain.service.getevents;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;

@Service
@Qualifier("classified")
public class ClassifiedGetEventsOperation implements GetEventsOperation {

    private final GetEventsOperation getEventsOperation;
    private final SecurityClassificationService classificationService;

    public ClassifiedGetEventsOperation(@Qualifier("default") GetEventsOperation getEventsOperation,
                                        SecurityClassificationService classificationService) {

        this.getEventsOperation = getEventsOperation;
        this.classificationService = classificationService;
    }

    @Override
    public List<AuditEvent> getEvents(CaseDetails caseDetails) {
        final List<AuditEvent> events = getEventsOperation.getEvents(caseDetails);

        return secureEvents(caseDetails.getJurisdiction(), events);
    }

    @Override
    public List<AuditEvent> getEvents(String jurisdiction, String caseTypeId, String caseReference) {
        return secureEvents(jurisdiction, getEventsOperation.getEvents(jurisdiction, caseTypeId, caseReference));
    }

    @Override
    public Optional<AuditEvent> getEvent(String jurisdiction, String caseTypeId, Long eventId) {
        return getEventsOperation.getEvent(jurisdiction, caseTypeId, eventId).flatMap(
            event -> secureEvent(jurisdiction, event));
    }

    private Optional<AuditEvent> secureEvent(String jurisdiction, AuditEvent event) {
        return secureEvents(jurisdiction, singletonList(event)).stream().findFirst();
    }

    private List<AuditEvent> secureEvents(String jurisdiction, List<AuditEvent> events) {
        if (null == events) {
            return Lists.newArrayList();
        }

        return classificationService.applyClassification(jurisdiction, events);
    }
}
