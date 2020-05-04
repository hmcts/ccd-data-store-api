package uk.gov.hmcts.ccd.domain.service.common;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.InvalidDefinitionException;

import javax.inject.Named;
import javax.inject.Singleton;
import static uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition.ANY;

@Named
@Singleton
public class EventTriggerService {
    public CaseEventDefinition findCaseEvent(final CaseTypeDefinition caseTypeDefinition,
                                             final String eventId) {
        return caseTypeDefinition.findCaseEvent(eventId).orElse(null);
    }

    public Boolean isPreStateValid(final String preStateId,
                                   final CaseEventDefinition caseEventDefinition) {
        if (caseEventDefinition.getPreStates() == null) {
            throw new InvalidDefinitionException(caseEventDefinition.getId() + " does not have any pre-states defined.");
        }
        return (preStateId == null && caseEventDefinition.getPreStates().isEmpty())
            || caseEventDefinition.getPreStates()
                .stream()
                .anyMatch(validState -> validState.equalsIgnoreCase(preStateId)
                    || StringUtils.equalsIgnoreCase(ANY, validState))
            ;
    }

    public Boolean isPreStateEmpty(final CaseEventDefinition caseEventDefinition) {
        if (caseEventDefinition.getPreStates() == null) {
            throw new InvalidDefinitionException(caseEventDefinition.getId() + " does not have any pre-states defined.");
        }
        return caseEventDefinition.getPreStates().isEmpty();
    }
}
