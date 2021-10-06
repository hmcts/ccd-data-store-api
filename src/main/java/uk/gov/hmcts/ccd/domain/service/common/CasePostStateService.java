package uk.gov.hmcts.ccd.domain.service.common;

import java.util.List;
import javax.inject.Inject;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.enablingcondition.PrioritiseEnablingCondition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;

@Service
public class CasePostStateService {

    private final PrioritiseEnablingCondition prioritiseEnablingCondition;
    private final CasePostStateEvaluationService casePostStateEvaluationService;

    @Inject
    public CasePostStateService(PrioritiseEnablingCondition prioritiseEnablingCondition,
                                CasePostStateEvaluationService casePostStateEvaluationService) {
        this.prioritiseEnablingCondition = prioritiseEnablingCondition;
        this.casePostStateEvaluationService = casePostStateEvaluationService;
    }

    public String evaluateCaseState(CaseEventDefinition caseEventDefinition, CaseDetails caseDetails) {
        List<EventPostStateDefinition> eventPostStateDefinitions = prioritiseEnablingCondition
            .prioritiseEventPostStates(caseEventDefinition.getPostStates());
        return casePostStateEvaluationService
            .evaluatePostStateCondition(eventPostStateDefinitions, caseDetails.getCaseDataAndMetadata());
    }

}
