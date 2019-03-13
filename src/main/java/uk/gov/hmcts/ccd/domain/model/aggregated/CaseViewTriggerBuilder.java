package uk.gov.hmcts.ccd.domain.model.aggregated;

public class CaseViewTriggerBuilder {
    private final CaseViewTrigger caseViewTrigger;

    private CaseViewTriggerBuilder() {
        this.caseViewTrigger = new CaseViewTrigger();
    }

    public CaseViewTriggerBuilder withId(String id) {
        this.caseViewTrigger.setId(id);
        return this;
    }

    public CaseViewTriggerBuilder withName(String name) {
        this.caseViewTrigger.setName(name);
        return this;
    }

    public CaseViewTriggerBuilder withDescription(String description) {
        this.caseViewTrigger.setDescription(description);
        return this;
    }

    public CaseViewTriggerBuilder withOrder(Integer order) {
        this.caseViewTrigger.setOrder(order);
        return this;
    }

    public CaseViewTrigger build() {
        return caseViewTrigger;
    }

    public static CaseViewTriggerBuilder anCaseViewTrigger() {
        return new CaseViewTriggerBuilder();
    }

}
