package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.ToString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

    public CaseDataContent() {
    /*
        Jackson required
     */
    }

    public CaseDataContent(Event event, Map<String, JsonNode> data, Map<String, JsonNode> securityClassification, String token, Boolean ignoreWarning) {
        this.event = event;
        this.data = data;
        this.securityClassification = securityClassification;
        this.token = token;
        this.ignoreWarning = ignoreWarning;
    }

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

    public CaseDataContent setIgnoreWarning(Boolean ignoreWarning) {
        this.ignoreWarning = ignoreWarning;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CaseDataContent that = (CaseDataContent) o;

        return new EqualsBuilder()
            .append(event, that.event)
            .append(data, that.data)
            .append(securityClassification, that.securityClassification)
            .append(token, that.token)
            .append(ignoreWarning, that.ignoreWarning)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(event)
            .append(data)
            .append(securityClassification)
            .append(token)
            .append(ignoreWarning)
            .toHashCode();
    }
}
