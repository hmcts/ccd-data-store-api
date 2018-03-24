package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CaseView {
    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("case_type")
    private CaseViewType caseType;
    private ProfileCaseState state;
    private String[] channels;
    private CaseViewTab[] tabs;
    private CaseViewTrigger[] triggers;
    private CaseViewEvent[] events;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public CaseViewType getCaseType() {
        return caseType;
    }

    public void setCaseType(CaseViewType caseType) {
        this.caseType = caseType;
    }

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

    public CaseViewTab[] getTabs() {
        return tabs;
    }

    public void setTabs(CaseViewTab[] tabs) {
        this.tabs = tabs;
    }

    public CaseViewTrigger[] getTriggers() {
        return triggers;
    }

    public void setTriggers(CaseViewTrigger[] triggers) {
        this.triggers = triggers;
    }

    public CaseViewEvent[] getEvents() {
        return events;
    }

    public void setEvents(CaseViewEvent[] events) {
        this.events = events;
    }
}
