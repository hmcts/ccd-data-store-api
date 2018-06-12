package uk.gov.hmcts.ccd.domain.service.listevents;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;

@Service
@Qualifier("classified")
public class ClassifiedListEventsOperation implements ListEventsOperation {

    private static final String EVENT_NOT_FOUND = "Case event not found";

    private final ListEventsOperation listEventsOperation;
    private final SecurityClassificationService classificationService;

    public ClassifiedListEventsOperation(@Qualifier("default") ListEventsOperation listEventsOperation,
                                         SecurityClassificationService classificationService) {

        this.listEventsOperation = listEventsOperation;
        this.classificationService = classificationService;
    }

    @Override
    public List<AuditEvent> execute(CaseDetails caseDetails) {
        final List<AuditEvent> events = listEventsOperation.execute(caseDetails);

        return secureEvents(caseDetails.getJurisdiction(), events);
    }

    @Override
    public List<AuditEvent> execute(String jurisdiction, String caseTypeId, String caseReference) {
        return secureEvents(jurisdiction, listEventsOperation.execute(jurisdiction, caseTypeId, caseReference));
    }

    @Override
    public AuditEvent execute(String jurisdiction, String caseTypeId, Long eventId) {
        return Optional.of(listEventsOperation.execute(jurisdiction, caseTypeId, eventId))
            .map(event -> secureEvent(jurisdiction, event))
            .orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND));
    }

    private AuditEvent secureEvent(String jurisdiction, AuditEvent event) {
        return secureEvents(jurisdiction, singletonList(event)).stream().findFirst().orElse(null);
    }

    private List<AuditEvent> secureEvents(String jurisdiction, List<AuditEvent> events) {
        if (null == events) {
            return Lists.newArrayList();
        }

        return classificationService.applyClassification(jurisdiction, events);
    }
}
