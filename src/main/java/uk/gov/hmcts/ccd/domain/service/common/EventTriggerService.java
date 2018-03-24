package uk.gov.hmcts.ccd.domain.service.common;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.endpoint.exceptions.InvalidDefinitionException;

import javax.inject.Named;
import javax.inject.Singleton;
import static uk.gov.hmcts.ccd.domain.model.definition.CaseState.ANY;

@Named
@Singleton
public class EventTriggerService {
    public CaseEvent findCaseEvent(final CaseType caseType,
                                   final String eventId) {
        return caseType
            .getEvents()
            .stream()
            .filter(event -> event.getId().equalsIgnoreCase(eventId))
            .findFirst()
            .orElse(null);
    }

    public Boolean isPreStateValid(final String preStateId,
                                   final CaseEvent caseEvent) {
        if (caseEvent.getPreStates() == null) {
            throw new InvalidDefinitionException(caseEvent.getId() + " does not have any pre-states defined.");
        }
        return (preStateId == null && caseEvent.getPreStates().isEmpty())
            || caseEvent.getPreStates()
                .stream()
                .anyMatch(validState -> validState.equalsIgnoreCase(preStateId)
                    || StringUtils.equalsIgnoreCase(ANY, validState))
            ;
    }

    public Boolean isPreStateEmpty(final CaseEvent caseEvent) {
        if (caseEvent.getPreStates() == null) {
            throw new InvalidDefinitionException(caseEvent.getId() + " does not have any pre-states defined.");
        }
        return caseEvent.getPreStates().isEmpty();
    }
}
