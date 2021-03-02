package uk.gov.hmcts.ccd.domain.model.std.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static org.apache.commons.lang.RandomStringUtils.random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;

class EventValidatorTest {

    private EventValidator underTest;
    private static final Integer SUMMARY_MAX_LENGTH = 1024;
    private static final Integer DESCRIPTION_MAX_LENGTH = 65536;

    @BeforeEach
    void init() {
        underTest = new EventValidator();
    }

    @Test
    @DisplayName("throws a ValidationException when the event is null")
    void validateNullEvent() {
        final ValidationException exception = assertThrows(ValidationException.class, () -> underTest.validate(null));
        assertEquals("Cannot create event because event is not specified", exception.getMessage());
    }

    @Test
    @DisplayName("throws a ValidationException when the event id is null")
    void validateNullEventId() {
        final ValidationException exception = assertThrows(ValidationException.class,
            () -> underTest.validate(anEvent().build()));
        assertEquals("Cannot create event because event is not specified", exception.getMessage());
    }

    @Test
    @DisplayName("throws a ValidationException when the event summary is too long")
    void validateEventWithSummaryTooLong() {
        Event event = anEvent().build();
        event.setEventId("");
        event.setSummary(random(SUMMARY_MAX_LENGTH + 1));

        final ValidationException exception = assertThrows(ValidationException.class, () -> underTest.validate(event));
        assertEquals("An event summary must be less than 1024 characters", exception.getMessage());
    }

    @Test
    @DisplayName("throws a ValidationException when the event description is too long")
    void validateEventWithDescriptionTooLong() {
        Event event = anEvent().build();
        event.setEventId("");
        event.setSummary(random(SUMMARY_MAX_LENGTH));
        event.setDescription(random(DESCRIPTION_MAX_LENGTH + 1));

        final ValidationException exception = assertThrows(ValidationException.class, () -> underTest.validate(event));
        assertEquals("An event description must be less than 65536 characters", exception.getMessage());
    }

    @Test
    @DisplayName("validator validates successfully; both summary and description are optional")
    void validateEvent() {
        Event event = anEvent().build();
        event.setEventId("");
        // and both event summary and description are null

        underTest.validate(event);
    }
}
