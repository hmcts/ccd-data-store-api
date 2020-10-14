package uk.gov.hmcts.ccd.domain.enablingcondition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;

@Component
public class PrioritiseEnablingCondition {

    public List<EventPostStateDefinition> prioritiseEventPostStates(List<EventPostStateDefinition> eventPostStates) {
        List<EventPostStateDefinition> cloned = new ArrayList<>(eventPostStates);
        cloned.sort(new PostStateComparator());
        return cloned;
    }

    private class PostStateComparator implements Comparator<EventPostStateDefinition> {

        @Override
        public int compare(EventPostStateDefinition first, EventPostStateDefinition second) {
            return first.getPriority().compareTo(second.getPriority());
        }
    }
}
