package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.ToString;

import java.util.Map;

@ToString
public class CaseDataContent {
    private Event event;
    private Map<String, JsonNode> data;
    @JsonProperty("security_classification")
    private Map<String, JsonNode> securityClassification;
    @JsonProperty("event_token")
    private String token;
    @JsonProperty("ignore_warning")
    private Boolean ignoreWarning;

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Map<String, JsonNode> getData() {
        return data;
    }

    public Map<String, JsonNode> getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(Map<String, JsonNode> securityClassification) {
        this.securityClassification = securityClassification;
    }

    public void setData(Map<String, JsonNode> data) {
        this.data = data;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getIgnoreWarning() {
        return ignoreWarning;
    }

    public void setIgnoreWarning(Boolean ignoreWarning) {
        this.ignoreWarning = ignoreWarning;
    }

}
