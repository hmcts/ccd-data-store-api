package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;
import java.util.Map;

@SuppressWarnings("checkstyle:SummaryJavadoc") // Javadoc predates checkstyle implementation in module
public class MessageInformation extends Event {
    @JsonIgnore
    private Long id;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("jurisdiction_id")
    private String jurisdictionId;
    @JsonProperty("case_type_id")
    private String caseTypeId;
    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("event_timestamp")
    private LocalDateTime eventTimestamp;
    @JsonProperty("event_instance_id")
    private String eventInstanceId;
    @JsonProperty("event_id")
    private String eventId;
    @JsonProperty("previous_state_id")
    private String previousStateId;
    @JsonProperty("new_state_id")
    private String newStateId;
    @JsonProperty("additional_data")
    @ApiModelProperty("A JSON object (for future use) that contains additional case fields that have been configured "
        + "to be included in the event information that is published.")
    private Map<String, JsonNode> data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

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

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getEventInstanceId() {
        return eventInstanceId;
    }

    public void setEventInstanceId(String eventInstanceId) {
        this.eventInstanceId = eventInstanceId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getPreviousStateId() {
        return previousStateId;
    }

    public void setPreviousStateId(String previousStateId) {
        this.previousStateId = previousStateId;
    }

    public String getNewStateId() {
        return newStateId;
    }

    public void setNewStateId(String newStateId) {
        this.newStateId = newStateId;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Map<String, JsonNode> getData() {
        return data;
    }

    public void setData(Map<String, JsonNode> data) {
        this.data = data;
    }

}
