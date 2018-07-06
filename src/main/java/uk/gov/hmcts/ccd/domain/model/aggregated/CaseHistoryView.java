package uk.gov.hmcts.ccd.domain.model.aggregated;

public class CaseHistoryView extends AbstractCaseView {

    private CaseViewEvent event;

    public CaseViewEvent getEvent() {
        return event;
    }

    public void setEvent(CaseViewEvent event) {
        this.event = event;
    }
}
