package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DefaultSettings {
    @JsonProperty("workbasket")
    private WorkbasketDefault workbasketDefault;

    public WorkbasketDefault getWorkbasketDefault() {
        return workbasketDefault;
    }

    public void setWorkbasketDefault(WorkbasketDefault workbasketDefault) {
        this.workbasketDefault = workbasketDefault;
    }

    @Override
    public String toString() {
        return "DefaultSettings{" +
               "workbasketDefault=" + workbasketDefault +
               '}';
    }
}
