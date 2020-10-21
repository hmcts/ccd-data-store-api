package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.enablingcondition.PrioritiseEnablingCondition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;

@Service
public class CasePostStateService {

    private final PrioritiseEnablingCondition prioritiseEnablingCondition;
    private CasePostStateEvaluationService casePostStateEvaluationService;

    @Inject
    public CasePostStateService(PrioritiseEnablingCondition prioritiseEnablingCondition,
                                CasePostStateEvaluationService casePostStateEvaluationService) {
        this.prioritiseEnablingCondition = prioritiseEnablingCondition;
        this.casePostStateEvaluationService = casePostStateEvaluationService;
    }

    public String evaluateCaseState(CaseEventDefinition caseEventDefinition, CaseDetails caseDetails) {
        List<EventPostStateDefinition> eventPostStateDefinitions = prioritiseEnablingCondition
            .prioritiseEventPostStates(caseEventDefinition.getPostStates());
        Map<String, JsonNode> caseEventData = caseDetails.getCaseEventData(getCaseFieldIds(caseEventDefinition));
        String postStateReference = casePostStateEvaluationService
            .evaluatePostStateCondition(eventPostStateDefinitions, caseEventData);
        return postStateReference;
    }

    private Set<String> getCaseFieldIds(CaseEventDefinition caseEventDefinition) {
        return caseEventDefinition
            .getCaseFields()
            .stream()
            .map(caseField -> caseField.getCaseFieldId())
            .collect(Collectors.toSet());
    }

}
