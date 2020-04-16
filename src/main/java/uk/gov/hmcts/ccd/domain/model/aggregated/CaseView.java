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

    public CaseViewActionableEvent[] getTriggers() {
        return triggers;
    }

    public void setTriggers(CaseViewActionableEvent[] triggers) {
        this.triggers = triggers;
    }

    public CaseViewEvent[] getEvents() {
        return events;
    }

    public void setEvents(CaseViewEvent[] events) {
        this.events = events;
    }
}
