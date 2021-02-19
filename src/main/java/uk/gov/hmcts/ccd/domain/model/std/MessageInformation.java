package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

@SuppressWarnings("checkstyle:SummaryJavadoc") // Javadoc predates checkstyle implementation in module
public class MessageInformation {
    @JsonIgnore
    private Long id;
    @JsonProperty("UserId")
    private String userId;
    @JsonProperty("JurisdictionId")
    private String jurisdictionId;
    @JsonProperty("CaseTypeId")
    private String caseTypeId;
    @JsonProperty("CaseId")
    private String caseId;
    @JsonProperty("EventTimeStamp")
    private LocalDateTime eventTimestamp;
    @JsonProperty("EventInstanceId")
    private Long eventInstanceId;
    @JsonProperty("EventId")
    private String eventId;
    @JsonProperty("PreviousStateId")
    private String previousStateId;
    @JsonProperty("NewStateId")
    private String newStateId;
    @JsonProperty("AdditionalData")
    @ApiModelProperty("A JSON object (for future use) that contains additional case fields that have been configured "
        + "to be included in the event information that is published.")
    private AdditionalMessageInformation data;

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

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public String getJurisdictionId() {
        return jurisdictionId;
    }

    public void setJurisdictionId(String jurisdictionId) {
        this.jurisdictionId = jurisdictionId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public Long getEventInstanceId() {
        return eventInstanceId;
    }

    public void setEventInstanceId(Long eventInstanceId) {
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

    public AdditionalMessageInformation getData() {
        return data;
    }

    public void setData(AdditionalMessageInformation data) {
        this.data = data;
    }

}
