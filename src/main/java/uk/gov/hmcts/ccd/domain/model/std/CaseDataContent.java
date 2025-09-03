package uk.gov.hmcts.ccd.domain.model.std;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.ToString;
import uk.gov.hmcts.ccd.domain.service.common.JcLogger;

@ToString
public class CaseDataContent {
    private final JcLogger jclogger = new JcLogger("CaseDataContent", true);

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

    private String[] getStringArray() {
        return new String[] {
            event.toString(),
            data.toString(),
            eventData.toString(),
            securityClassification,
            dataClassification.toString(),
            token,
            ignoreWarning.toString(),
            draftId,
            caseReference,
            onBehalfOfUserToken,
            onBehalfOfId,
            supplementaryDataRequest.toString()
        };
    }

    private void jcdebug(final String method) {
        try {
            final String thisAsString = jclogger.printObjectToString(getStringArray());
            if (thisAsString.contains("Confidential or sensitive information")) {
                jclogger.jclog(method + " " + thisAsString);
                jclogger.jclog(method + " CALL STACK = " + jclogger.getCallStackAsString());
            }
        } catch (Exception e) {
            // Do nothing
        }
    }

    public Event getEvent() {
        jcdebug("getEvent()");
        return event;
    }

    @JsonIgnore
    public String getEventId() {
        jcdebug("getEventId()");
        return event.getEventId();
    }

    public void setEvent(Event event) {
        jcdebug("setEvent()");
        this.event = event;
    }

    public Map<String, JsonNode> getData() {
        jcdebug("getData()");
        return data;
    }

    public void setData(Map<String, JsonNode> data) {
        jcdebug("setData()");
        this.data = data;
    }

    public Map<String, JsonNode> getEventData() {
        jcdebug("getEventData()");
        return eventData;
    }

    public void setEventData(Map<String, JsonNode> eventData) {
        jcdebug("setEventData()");
        this.eventData = eventData;
    }

    public String getSecurityClassification() {
        jcdebug("getSecurityClassification()");
        return securityClassification;
    }

    public void setSecurityClassification(String securityClassification) {
        jcdebug("setSecurityClassification()");
        this.securityClassification = securityClassification;
    }

    public Map<String, JsonNode> getDataClassification() {
        jcdebug("getDataClassification()");
        return dataClassification;
    }

    public void setDataClassification(Map<String, JsonNode> dataClassification) {
        jcdebug("setDataClassification()");
        this.dataClassification = dataClassification;
    }

    public String getToken() {
        jcdebug("getToken()");
        return token;
    }

    public void setToken(String token) {
        jcdebug("setToken()");
        this.token = token;
    }

    public Boolean getIgnoreWarning() {
        jcdebug("getIgnoreWarning()");
        return ignoreWarning == null ? Boolean.FALSE : ignoreWarning;
    }

    public void setIgnoreWarning(Boolean ignoreWarning) {
        jcdebug("setIgnoreWarning()");
        this.ignoreWarning = ignoreWarning;
    }

    public String getDraftId() {
        jcdebug("getDraftId()");
        return draftId;
    }

    public void setDraftId(String draftId) {
        jcdebug("setDraftId()");
        this.draftId = draftId;
    }

    public String getCaseReference() {
        jcdebug("getCaseReference()");
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        jcdebug("setCaseReference()");
        this.caseReference = caseReference;
    }

    public String getOnBehalfOfUserToken() {
        jcdebug("getOnBehalfOfUserToken()");
        return onBehalfOfUserToken;
    }

    public void setOnBehalfOfUserToken(String onBehalfOfUserToken) {
        jcdebug("setOnBehalfOfUserToken()");
        this.onBehalfOfUserToken = onBehalfOfUserToken;
    }

    public String getOnBehalfOfId() {
        jcdebug("getOnBehalfOfId()");
        return onBehalfOfId;
    }

    public void setOnBehalfOfId(String onBehalfOfId) {
        jcdebug("setOnBehalfOfId()");
        this.onBehalfOfId = onBehalfOfId;
    }

    public Map<String, Map<String, Object>> getSupplementaryDataRequest() {
        jcdebug("getSupplementaryDataRequest()");
        return supplementaryDataRequest;
    }

    public void setSupplementaryDataRequest(Map<String, Map<String, Object>> supplementaryDataRequest) {
        jcdebug("setSupplementaryDataRequest()");
        this.supplementaryDataRequest = supplementaryDataRequest;
    }
}
