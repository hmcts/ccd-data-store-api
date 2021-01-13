package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.enablingcondition.EnablingConditionParser;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

@Service
public class CasePostStateEvaluationService {

    private final EnablingConditionParser enablingConditionParser;

    @Inject
    public CasePostStateEvaluationService(EnablingConditionParser enablingConditionParser) {
        this.enablingConditionParser = enablingConditionParser;
    }

    public String evaluatePostStateCondition(List<EventPostStateDefinition> eventPostStateDefinitions,
                                                       Map<String, JsonNode> caseEventData) {
        for (EventPostStateDefinition eventPostStateDefinition : eventPostStateDefinitions) {
            if (!eventPostStateDefinition.isDefault()) {
                Boolean conditionMatched = this.enablingConditionParser.evaluate(
                    eventPostStateDefinition.getEnablingCondition(),
                    caseEventData);
                if (conditionMatched) {
                    return eventPostStateDefinition.getPostStateReference();
                }
            }
        }
        return getDefaultPostStateReference(eventPostStateDefinitions);
    }

    private String getDefaultPostStateReference(List<EventPostStateDefinition> eventPostStateDefinitions) {
        return eventPostStateDefinitions
            .stream()
            .filter(postState -> postState.isDefault())
            .map(postState -> postState.getPostStateReference())
            .findAny()
            .orElseThrow(() -> new ServiceException("No default post state exists"));
    }
}
