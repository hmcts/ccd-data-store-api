package uk.gov.hmcts.ccd.domain.service.startevent;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

@Service
@Qualifier("classified")
public class ClassifiedStartEventOperation implements StartEventOperation {
    private final StartEventOperation startEventOperation;
    private final SecurityClassificationService classificationService;

    public ClassifiedStartEventOperation(@Qualifier("default") StartEventOperation startEventOperation,
                                         SecurityClassificationService classificationService) {

        this.startEventOperation = startEventOperation;
        this.classificationService = classificationService;
    }

    @Override
    public StartEventTrigger triggerStartForCaseType(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, Boolean ignoreWarning) {
        return startEventOperation.triggerStartForCaseType(uid,
                                                           jurisdictionId,
                                                           caseTypeId,
                                                           eventTriggerId,
                                                           ignoreWarning);
    }

    @Override
    public StartEventTrigger triggerStartForCase(String uid, String jurisdictionId, String caseTypeId, String caseReference, String eventTriggerId, Boolean ignoreWarning) {
        return applyClassificationIfCaseDetailsExist(startEventOperation.triggerStartForCase(uid,
                                                                                      jurisdictionId,
                                                                                      caseTypeId,
                                                                                      caseReference,
                                                                                      eventTriggerId,
                                                                                      ignoreWarning));
    }

    @Override
    public StartEventTrigger triggerStartForDraft(String uid, String jurisdictionId, String caseTypeId, String draftReference, String eventTriggerId, Boolean ignoreWarning) {
        return null;
    }

    private StartEventTrigger applyClassificationIfCaseDetailsExist(StartEventTrigger startEventTrigger) {
        if (null != startEventTrigger.getCaseDetails()) {
            startEventTrigger.setCaseDetails(classificationService.applyClassification(startEventTrigger.getCaseDetails()).orElse(null));
        }
        return startEventTrigger;
    }
}
