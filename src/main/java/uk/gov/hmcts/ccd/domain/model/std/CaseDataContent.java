package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.ToString;

import java.util.Map;

@ToString
public class CaseDataContent {
    private Event event;
    private Map<String, JsonNode> data;

    @JsonProperty("security_classification")
    private String securityClassification;

    @JsonProperty("data_classification")
    private Map<String, JsonNode> dataClassification;

    @JsonProperty("event_token")
    private String token;

    @JsonProperty("ignore_warning")
    private Boolean ignoreWarning;

    @JsonProperty("draft_id")
    private String draftId;

    public Event getEvent() {
        return event;
    }

    @JsonIgnore
    public String getEventId() {
        return event.getEventId();
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Map<String, JsonNode> getData() {
        return data;
    }

    public String getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(String securityClassification) {
        this.securityClassification = securityClassification;
    }

    public void setData(Map<String, JsonNode> data) {
        this.data = data;
    }

    public Map<String, JsonNode> getDataClassification() {
        return dataClassification;
    }

    public void setDataClassification(Map<String, JsonNode> dataClassification) {
        this.dataClassification = dataClassification;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getIgnoreWarning() {
        return ignoreWarning == null ? Boolean.FALSE : ignoreWarning;
    }

    public void setIgnoreWarning(Boolean ignoreWarning) {
        this.ignoreWarning = ignoreWarning;
    }

    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }
}
