package uk.gov.hmcts.ccd.domain.casestate;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;

@Component
public class EnablingConditionSorter {

    public void sortEventPostStates(List<EventPostStateDefinition> eventPostStates) {
        if (eventPostStates != null) {
            eventPostStates.sort(new PostStateComparator());
        }
    }

    private class PostStateComparator implements Comparator<EventPostStateDefinition> {

        @Override
        public int compare(EventPostStateDefinition first, EventPostStateDefinition second) {
            if (first.getPriority() != null) {
                return first.getPriority().compareTo(second.getPriority());
            } else if (second.getPriority() != null) {
                return second.getPriority().compareTo(first.getPriority());
            }
            return 0;
        }
    }
}
