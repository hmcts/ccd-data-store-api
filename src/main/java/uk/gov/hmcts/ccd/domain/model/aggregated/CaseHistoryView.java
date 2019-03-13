package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CaseHistoryView extends AbstractCaseView {

    private CaseViewEvent event;

    @JsonIgnore
    public Long getEventId() {
        return event.getId();
    }

    public CaseViewEvent getEvent() {
        return event;
    }

    public void setEvent(CaseViewEvent event) {
        this.event = event;
    }
}
