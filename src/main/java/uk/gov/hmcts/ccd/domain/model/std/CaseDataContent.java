package uk.gov.hmcts.ccd.domain.model.std;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.ToString;

@ToString
public class CaseDataContent {
    private Event event;
    private Map<String, JsonNode> data;

    // full event data payload
    @JsonProperty("event_data")
    private Map<String, JsonNode> eventData;

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

    @JsonProperty("case_reference")
    private String caseReference;

    @JsonProperty("on_behalf_of_token")
    private String onBehalfOfUserToken;

    @JsonProperty("on_behalf_of_id")
    private String onBehalfOfId;

    @JsonProperty("supplementary_data_request")
    private Map<String, Map<String, Object>> supplementaryDataRequest;

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

    public void setData(Map<String, JsonNode> data) {
        this.data = data;
    }

    public Map<String, JsonNode> getEventData() {
        return eventData;
    }

    public void setEventData(Map<String, JsonNode> eventData) {
        this.eventData = eventData;
    }

    public String getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(String securityClassification) {
        this.securityClassification = securityClassification;
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

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    public String getOnBehalfOfUserToken() {
        return onBehalfOfUserToken;
    }

    public void setOnBehalfOfUserToken(String onBehalfOfUserToken) {
        this.onBehalfOfUserToken = onBehalfOfUserToken;
    }

    public String getOnBehalfOfId() {
        return onBehalfOfId;
    }

    public void setOnBehalfOfId(String onBehalfOfId) {
        this.onBehalfOfId = onBehalfOfId;
    }

    public Map<String, Map<String, Object>> getSupplementaryDataRequest() {
        return supplementaryDataRequest;
    }

    public void setSupplementaryDataRequest(Map<String, Map<String, Object>> supplementaryDataRequest) {
        this.supplementaryDataRequest = supplementaryDataRequest;
    }
}
