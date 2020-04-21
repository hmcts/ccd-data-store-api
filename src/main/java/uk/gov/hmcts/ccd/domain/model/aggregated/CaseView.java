package uk.gov.hmcts.ccd.domain.model.aggregated;

public class CaseView extends AbstractCaseView {
    private ProfileCaseState state;
    private String[] channels;
    private CaseViewActionableEvent[] triggers;
    private CaseViewEvent[] events;

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
        return triggers;
    }

    public void setActionableEvents(CaseViewActionableEvent[] actionableEvents) {
        this.triggers = actionableEvents;
    }

    public CaseViewEvent[] getEvents() {
        return events;
    }

    public void setEvents(CaseViewEvent[] events) {
        this.events = events;
    }
}
