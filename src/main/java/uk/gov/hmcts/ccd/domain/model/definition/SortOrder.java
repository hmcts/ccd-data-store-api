package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class SortOrder implements Serializable {

    @JsonProperty("direction")
    private String direction;
    @JsonProperty("priority")
    private Integer priority;

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
