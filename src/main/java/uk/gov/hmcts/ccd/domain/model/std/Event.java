package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import uk.gov.hmcts.ccd.util.EventDescriptionRedactor;

@ToString
public class Event {
    @JsonProperty("id")
    private String eventId;
    @JsonProperty("summary")
    private String summary;

    private String description;

    private final EventDescriptionRedactor redactor = new EventDescriptionRedactor();

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
        return redactor.redact(description);
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

}
