package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CaseHistoryView extends AbstractCaseView {

    private CaseViewEvent event;

    @JsonIgnore
    public String getEventId() {
        return event.getEventId();
    }

    public CaseViewEvent getEvent() {
        return event;
    }

    public void setEvent(CaseViewEvent event) {
        this.event = event;
    }
}
