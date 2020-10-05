package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.casestate.EnablingConditionParser;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;

@Service
public class StateReferenceService {

    private final EnablingConditionParser enablingConditionParser;

    @Inject
    public StateReferenceService(EnablingConditionParser enablingConditionParser) {
        this.enablingConditionParser = enablingConditionParser;
    }

    public Optional<String> evaluatePostStateCondition(List<EventPostStateDefinition> eventPostStateDefinitions,
                                                       Map<String, JsonNode> caseEventData) {
        List<EventPostStateDefinition> postStateDefinitions = Optional
            .ofNullable(eventPostStateDefinitions)
            .orElse(new ArrayList<>());

        for (EventPostStateDefinition eventPostStateDefinition : postStateDefinitions) {
            String enablingCondition = eventPostStateDefinition.getEnablingCondition();
            if (enablingCondition != null) {
                Boolean conditionMatched = this.enablingConditionParser.evaluate(
                    eventPostStateDefinition.getEnablingCondition(),
                    caseEventData);
                if (conditionMatched) {
                    return Optional.of(eventPostStateDefinition.getPostStateReference());
                }
            }
        }
        return getDefaultPostStateReference(postStateDefinitions);
    }

    private Optional<String> getDefaultPostStateReference(List<EventPostStateDefinition> eventPostStateDefinitions) {
        return eventPostStateDefinitions
            .stream()
            .filter(postState -> postState.getEnablingCondition() == null)
            .map(postState -> postState.getPostStateReference())
            .findAny();
    }
}
