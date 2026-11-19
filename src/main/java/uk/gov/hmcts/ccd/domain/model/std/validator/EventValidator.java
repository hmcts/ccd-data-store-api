package uk.gov.hmcts.ccd.domain.model.std.validator;

import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named
@Singleton
public class EventValidator {

    private static final Integer SUMMARY_MAX_LENGTH = 1024;
    private static final Integer DESCRIPTION_MAX_LENGTH = 65536;

    public static final String EVENT_NOT_SPECIFIED = "Cannot create event because event is not specified";
    public static final String SUMMARY_TOO_LONG = String.format(
        "An event summary must be less than %d characters", SUMMARY_MAX_LENGTH);
    public static final String DESCRIPTION_TOO_LONG = String.format(
        "An event description must be less than %d characters", DESCRIPTION_MAX_LENGTH);

    public void validate(final Event event) {

        if (event == null || event.getEventId() == null) {
            throw new ValidationException(EVENT_NOT_SPECIFIED);
        }

        if (null != event.getSummary() && SUMMARY_MAX_LENGTH < event.getSummary().length()) {
            throw new ValidationException(SUMMARY_TOO_LONG);
        }

        if (null != event.getDescription() && DESCRIPTION_MAX_LENGTH < event.getDescription().length()) {
            throw new ValidationException(DESCRIPTION_TOO_LONG);
        }
    }
}
