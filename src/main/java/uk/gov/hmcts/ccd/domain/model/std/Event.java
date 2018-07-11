package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@ToString
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

    public Event() {
        // Default constructor for JSON mapper
    }

    public Event(String eventId, String summary, String description) {
        this.eventId = eventId;
        this.summary = summary;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        return new EqualsBuilder()
            .append(eventId, event.eventId)
            .append(summary, event.summary)
            .append(description, event.description)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(eventId)
            .append(summary)
            .append(description)
            .toHashCode();
    }
}
