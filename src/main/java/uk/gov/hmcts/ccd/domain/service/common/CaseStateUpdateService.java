package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.casestate.EnablingConditionSorter;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;

@Service
public class CaseStateUpdateService {

    private final EnablingConditionSorter enablingConditionSorter;
    private StateReferenceService stateReferenceService;

    @Inject
    public CaseStateUpdateService(EnablingConditionSorter enablingConditionSorter,
                                  StateReferenceService stateReferenceService) {
        this.enablingConditionSorter = enablingConditionSorter;
        this.stateReferenceService = stateReferenceService;
    }

    public Optional<String> retrieveCaseState(CaseEventDefinition caseEventDefinition, CaseDetails caseDetails) {
        List<EventPostStateDefinition> eventPostStateDefinitions = caseEventDefinition.getPostStates();
        this.enablingConditionSorter.sortEventPostStates(eventPostStateDefinitions);
        Map<String, JsonNode> caseEventData = caseDetails.getCaseEventData(caseEventDefinition);
        Optional<String> postStateReference = this.stateReferenceService
            .evaluatePostStateCondition(eventPostStateDefinitions, caseEventData);
        return postStateReference;
    }

}
