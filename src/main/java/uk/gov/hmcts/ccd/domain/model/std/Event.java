package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import uk.gov.hmcts.ccd.domain.service.common.JcLogger;

@ToString
public class Event {
    private final JcLogger jclogger = new JcLogger("Event", true);

    @JsonProperty("id")
    private String eventId;
    @JsonProperty("summary")
    private String summary;
    @JsonProperty("description")
    private String description;

    private void jcdebug(final String method, final String value) {
        jclogger.jclog(method + " " + value);
        jclogger.jclog(method + " " + jclogger.getCallStackAsString());
    }

    public String getEventId() {
        jcdebug("getEventId()", eventId);
        return eventId;
    }

    public void setEventId(String eventId) {
        jcdebug("setEventId()", eventId);
        this.eventId = eventId;
    }

    public String getSummary() {
        jcdebug("getSummary()", summary);
        return summary;
    }

    public void setSummary(String summary) {
        jcdebug("setSummary()", summary);
        this.summary = summary;
    }

    public String getDescription() {
        jcdebug("getDescription()", description);
        return description;
    }

    public void setDescription(String description) {
        jcdebug("setDescription()", description);
        this.description = description;
    }

}
