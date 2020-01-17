package uk.gov.hmcts.ccd.domain.model.std;

public class EventBuilder {
    private final Event event;

    private EventBuilder() {
        this.event = new Event();
    }

    public EventBuilder withEventId(String eventId) {
        this.event.setEventId(eventId);
        return this;
    }

    public EventBuilder withSummary(String summary) {
        this.event.setSummary(summary);
        return this;
    }

    public EventBuilder withDescription(String description) {
        this.event.setDescription(description);
        return this;
    }

    public static EventBuilder anEvent() {
        return new EventBuilder();
    }

    public Event build() {
        return this.event;
    }
}
