package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class Event {
    @JsonProperty("id")
    private String eventId;
    @JsonProperty("summary")
    private String summary;
    @JsonProperty("description")
    private String description;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
