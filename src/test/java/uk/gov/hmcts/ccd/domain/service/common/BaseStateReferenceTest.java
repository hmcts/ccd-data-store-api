package uk.gov.hmcts.ccd.domain.service.common;

import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;

public class BaseStateReferenceTest {

    protected BaseStateReferenceTest(){
    }

    protected EventPostStateDefinition createEventPostStateDefinition(String postStateReference,
                                                                    String enablingCondition,
                                                                    int priority) {
        EventPostStateDefinition eventPostStateDefinition = new EventPostStateDefinition();
        eventPostStateDefinition.setPriority(priority);
        eventPostStateDefinition.setPostStateReference(postStateReference);
        eventPostStateDefinition.setEnablingCondition(enablingCondition);
        return eventPostStateDefinition;
    }

    protected List<EventPostStateDefinition> createPostStates() {
        List<EventPostStateDefinition> postStates = createEventPostStates();

        EventPostStateDefinition eventPostStateDefinition = createEventPostStateDefinition(
            "Test125",
            null,
            99);
        postStates.add(eventPostStateDefinition);
        return postStates;
    }

    protected List<EventPostStateDefinition> createEventPostStates() {
        List<EventPostStateDefinition> postStates = new ArrayList<>();
        EventPostStateDefinition eventPostStateDefinition = createEventPostStateDefinition(
            "Test123",
            "Field1=\'333\"",
            1);
        postStates.add(eventPostStateDefinition);
        eventPostStateDefinition = createEventPostStateDefinition(
            "Test124",
            "Field3=\'334\"",
            2);
        postStates.add(eventPostStateDefinition);
        return postStates;
    }
}
