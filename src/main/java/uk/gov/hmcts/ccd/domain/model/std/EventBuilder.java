package uk.gov.hmcts.ccd.domain.model.std;

public class EventBuilder {
    private String eventId;
    private String summary;
    private String description;

    public EventBuilder withEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    public EventBuilder withSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public EventBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public static EventBuilder anEvent() {
        return new EventBuilder();
    }

    public Event build() {
        return new Event(eventId, summary, description);
    }
}
