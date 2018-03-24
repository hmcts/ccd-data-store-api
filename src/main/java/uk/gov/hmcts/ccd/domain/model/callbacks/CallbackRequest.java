package uk.gov.hmcts.ccd.domain.model.callbacks;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

public class CallbackRequest {

    @JsonProperty("case_details")
    private CaseDetails caseDetails;

    @JsonProperty("case_details_before")
    private CaseDetails caseDetailsBefore;

    @JsonProperty("event_id")
    private String eventId;

    public CallbackRequest(final CaseDetails caseDetails,
                           final CaseDetails caseDetailsBefore,
                           final String eventId) {
        this.caseDetails = caseDetails;
        this.caseDetailsBefore = caseDetailsBefore;
        this.eventId = eventId;
    }

    public CaseDetails getCaseDetails() {
        return caseDetails;
    }

    public CaseDetails getCaseDetailsBefore() {
        return caseDetailsBefore;
    }

    public String getEventId() {
        return eventId;
    }
}
