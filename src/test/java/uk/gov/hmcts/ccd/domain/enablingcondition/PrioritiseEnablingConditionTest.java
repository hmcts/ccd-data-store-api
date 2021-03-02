package uk.gov.hmcts.ccd.domain.enablingcondition;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrioritiseEnablingConditionTest {

    private PrioritiseEnablingCondition prioritiseEnablingCondition;

    @BeforeEach
    void setUp() {
        this.prioritiseEnablingCondition = new PrioritiseEnablingCondition();
    }

    @Test
    void sortEventPostStates() {
        List<EventPostStateDefinition> eventPostStateDefinitionList = new ArrayList<>();
        eventPostStateDefinitionList.add(createEventPostState(
            "FieldA!=\"\" AND FieldB=\"I'm innocent\"",
            "ApprovalRequired", 2));

        eventPostStateDefinitionList.add(createEventPostState(
            "FieldC=\"*\" AND FieldD=\"Plea Entered\"",
            "ScheduleForHearing", 1));

        eventPostStateDefinitionList.add(createEventPostState(
            null,
            "ReadyForDirections", 99));
        eventPostStateDefinitionList = this.prioritiseEnablingCondition
            .prioritiseEventPostStates(eventPostStateDefinitionList);

        assertEquals(3, eventPostStateDefinitionList.size());
        assertEquals(1, eventPostStateDefinitionList.get(0).getPriority());
        assertEquals(2, eventPostStateDefinitionList.get(1).getPriority());
        assertEquals(99, eventPostStateDefinitionList.get(2).getPriority());
    }

    private EventPostStateDefinition createEventPostState(String enablingCondition,
                                                          String postStateReference,
                                                          Integer priority) {
        EventPostStateDefinition eventPostStateDefinition = new EventPostStateDefinition();
        eventPostStateDefinition.setEnablingCondition(enablingCondition);
        eventPostStateDefinition.setPostStateReference(postStateReference);
        eventPostStateDefinition.setPriority(priority);
        return eventPostStateDefinition;
    }
}
