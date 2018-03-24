package uk.gov.hmcts.ccd.domain.model.callbacks;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

public class StartEventTrigger {
    @JsonProperty("case_details")
    private CaseDetails caseDetails;
    @JsonProperty("event_id")
    private String eventId;
    private String token;

    public CaseDetails getCaseDetails() {
        return caseDetails;
    }

    public void setCaseDetails(CaseDetails caseDetails) {
        this.caseDetails = caseDetails;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
