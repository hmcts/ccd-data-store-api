package uk.gov.hmcts.ccd.domain.service.listevents;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.List;

@Service
@Qualifier("classified")
public class ClassifiedListEventsOperation implements ListEventsOperation {

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

        if (null == events) {
            return Lists.newArrayList();
        }

        return classificationService.applyClassification(caseDetails.getJurisdiction(), events);
    }
}
