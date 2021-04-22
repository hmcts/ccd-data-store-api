package uk.gov.hmcts.ccd.domain.service.getevents;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;

import static java.util.Collections.singletonList;

@Service
@Qualifier("classified")
public class ClassifiedGetEventsOperation implements GetEventsOperation {

    private final GetEventsOperation getEventsOperation;
    private final SecurityClassificationService classificationService;
    private final GetCaseOperation getCaseOperation;

    public ClassifiedGetEventsOperation(@Qualifier("default") GetEventsOperation getEventsOperation,
                                        SecurityClassificationService classificationService,
                                        @Qualifier(CreatorGetCaseOperation.QUALIFIER)
                                        final GetCaseOperation getCaseOperation) {

        this.getEventsOperation = getEventsOperation;
        this.classificationService = classificationService;
        this.getCaseOperation = getCaseOperation;
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
    public List<AuditEvent> getEvents(String caseReference) {
        return secureEvents(getEventsOperation.getEvents(caseReference), caseReference);
    }

    @Override
    public Optional<AuditEvent> getEvent(String jurisdiction, String caseTypeId, Long eventId) {
        return getEventsOperation.getEvent(jurisdiction, caseTypeId, eventId).flatMap(
            event -> secureEvent(jurisdiction, event));
    }

    private Optional<AuditEvent> secureEvent(String jurisdiction, AuditEvent event) {
        return secureEvents(jurisdiction, singletonList(event)).stream().findFirst();
    }

    private List<AuditEvent> secureEvents(List<AuditEvent> events, String caseReference) {
        CaseDetails caseDetails = getCaseOperation.execute(caseReference).orElse(null);
        if (null == caseDetails) {
            return Lists.newArrayList();
        }
        return classificationService.applyClassification(caseDetails.getJurisdiction(), events);
    }

    private List<AuditEvent> secureEvents(String jurisdiction, List<AuditEvent> events) {
        if (null == events) {
            return Lists.newArrayList();
        }

        return classificationService.applyClassification(jurisdiction, events);
    }
}
