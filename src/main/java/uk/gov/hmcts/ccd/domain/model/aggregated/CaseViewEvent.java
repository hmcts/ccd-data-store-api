package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.time.LocalDateTime;

public class CaseViewEvent {
    private Long id;

    private LocalDateTime timestamp;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_name")
    private String eventName;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_last_name")
    private String userLastName;

    @JsonProperty("user_first_name")
    private String userFirstName;

    private String summary;

    private String comment;

    @JsonProperty("state_name")
    private String stateName;

    @JsonProperty("state_id")
    private String stateId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public void setUserLastName(String userLastName) {
        this.userLastName = userLastName;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    public static CaseViewEvent createFrom(AuditEvent event) {
        CaseViewEvent caseEvent = new CaseViewEvent();
        caseEvent.setId(event.getId());
        caseEvent.setEventId(event.getEventId());
        caseEvent.setEventName(event.getEventName());
        caseEvent.setUserId(event.getUserId());
        caseEvent.setUserLastName(event.getUserLastName());
        caseEvent.setUserFirstName(event.getUserFirstName());
        caseEvent.setSummary(event.getSummary());
        caseEvent.setComment(event.getDescription());
        caseEvent.setTimestamp(event.getCreatedDate());
        caseEvent.setStateId(event.getStateId());
        caseEvent.setStateName(event.getStateName());
        return caseEvent;
    }
}
