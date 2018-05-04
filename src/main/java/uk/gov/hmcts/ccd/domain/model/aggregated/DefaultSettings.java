package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@ToString
public class DefaultSettings {
    @JsonProperty("workbasket")
    private WorkbasketDefault workbasketDefault;

    public WorkbasketDefault getWorkbasketDefault() {
        return workbasketDefault;
    }

    public void setWorkbasketDefault(WorkbasketDefault workbasketDefault) {
        this.workbasketDefault = workbasketDefault;
    }

}
