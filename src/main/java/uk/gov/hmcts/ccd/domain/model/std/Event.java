package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import uk.gov.hmcts.ccd.domain.service.common.JcLogger;
import uk.gov.hmcts.ccd.util.EventDescriptionRedactor;

@ToString
public class Event {
    private final JcLogger jclogger = new JcLogger("Event", true);

    @JsonProperty("id")
    private String eventId;
    @JsonProperty("summary")
    private String summary;

    private String description;

    private final EventDescriptionRedactor redactor = new EventDescriptionRedactor();

    // eventId is "caseworker-add-note" in the scenario.
    private void jcdebug(final String method, final String value) {
        if (value != null && value.length() > 0) {
            jclogger.jclog(method + " eventId = " + eventId + " , value = " + value);
            jclogger.jclog(method + " eventId = " + eventId + " , CALL STACK = " + jclogger.getCallStackAsString());
        }
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @JsonProperty("description")
    public String getDescription() {
        jcdebug("getDescription()", description);
        return redactor.redact(description);
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        jcdebug("setDescription()", description);
        this.description = description;
    }

}
