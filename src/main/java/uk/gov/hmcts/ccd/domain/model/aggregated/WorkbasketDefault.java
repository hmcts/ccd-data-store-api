package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class WorkbasketDefault {
    @JsonProperty("jurisdiction_id")
    private String jurisdictionId;
    @JsonProperty("case_type_id")
    private String caseTypeId;
    @JsonProperty("state_id")
    private String stateId;

    public String getJurisdictionId() {
        return jurisdictionId;
    }

    public void setJurisdictionId(String jurisdictionId) {
        this.jurisdictionId = jurisdictionId;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("jurisdictionId", jurisdictionId)
            .append("caseTypeId", caseTypeId)
            .append("stateId", stateId)
            .toString();
    }
}
