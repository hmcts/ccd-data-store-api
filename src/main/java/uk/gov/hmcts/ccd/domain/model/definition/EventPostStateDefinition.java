package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@ApiModel(description = "")
@NoArgsConstructor
public class EventPostStateDefinition implements Serializable {

    @JsonProperty("enabling_condition")
    private String enablingCondition;

    @JsonProperty("priority")
    private Integer priority;

    @JsonProperty("post_state_reference")
    private String postStateReference;

    public String getEnablingCondition() {
        return enablingCondition;
    }

    public void setEnablingCondition(String enablingCondition) {
        this.enablingCondition = enablingCondition;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getPostStateReference() {
        return postStateReference;
    }

    public void setPostStateReference(String postStateReference) {
        this.postStateReference = postStateReference;
    }

    @JsonIgnore
    public boolean isDefault() {
        return enablingCondition == null;
    }
}
