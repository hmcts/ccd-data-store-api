package uk.gov.hmcts.ccd.domain.model.aggregated;

public class CaseViewActionableEventBuilder {
    private final CaseViewActionableEvent caseViewActionableEvent;

    private CaseViewActionableEventBuilder() {
        this.caseViewActionableEvent = new CaseViewActionableEvent();
    }

    public CaseViewActionableEventBuilder withId(String id) {
        this.caseViewActionableEvent.setId(id);
        return this;
    }

    public CaseViewActionableEventBuilder withName(String name) {
        this.caseViewActionableEvent.setName(name);
        return this;
    }

    public CaseViewActionableEventBuilder withDescription(String description) {
        this.caseViewActionableEvent.setDescription(description);
        return this;
    }

    public CaseViewActionableEventBuilder withOrder(Integer order) {
        this.caseViewActionableEvent.setOrder(order);
        return this;
    }

    public CaseViewActionableEvent build() {
        return caseViewActionableEvent;
    }

    public static CaseViewActionableEventBuilder anCaseViewActionableEvent() {
        return new CaseViewActionableEventBuilder();
    }

}
