package uk.gov.hmcts.ccd.domain.model.aggregated;

public class CaseView extends AbstractCaseView {
    private ProfileCaseState state;
    private String[] channels;
    private CaseViewActionableEvent[] actionableEvents;
    private CaseViewEvent[] caseViewEvents;

    public ProfileCaseState getState() {
        return state;
    }

    public void setState(ProfileCaseState state) {
        this.state = state;
    }

    public String[] getChannels() {
        return channels;
    }

    public void setChannels(String[] channels) {
        this.channels = channels;
    }

    public CaseViewActionableEvent[] getActionableEvents() {
        return actionableEvents;
    }

    public void setActionableEvents(CaseViewActionableEvent[] actionableEvents) {
        this.actionableEvents = actionableEvents;
    }

    public CaseViewEvent[] getEvents() {
        return caseViewEvents;
    }

    public void setEvents(CaseViewEvent[] events) {
        this.caseViewEvents = events;
    }
}
