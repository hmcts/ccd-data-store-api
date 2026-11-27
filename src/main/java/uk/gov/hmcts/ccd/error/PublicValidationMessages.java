package uk.gov.hmcts.ccd.error;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.ccd.domain.model.std.validator.EventValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Set;

/** Explicitly reviewed public messages; dynamic exception diagnostics remain private. */
public final class PublicValidationMessages {
    private static final Set<String> BAD_REQUEST_MESSAGES = Set.of(
        ValidationMessages.CASE_REFERENCE_INVALID,
        ValidationMessages.CASE_ID_INVALID,
        ValidationMessages.CASE_TYPE_INVALID,
        ValidationMessages.START_TRIGGER_TOKEN_MISSING,
        ValidationMessages.START_TRIGGER_TOKEN_EXPIRED,
        ValidationMessages.ACCESS_INVALID,
        ValidationMessages.SORT_ORDER_INVALID,
        ValidationMessages.CASE_VERSION_INVALID,
        ValidationMessages.CATEGORY_ID_INVALID,
        "Field Names Invalid",
        "Enter valid tags for RichTextArea field",
        "Missing required case type. Please provide a case type or list of case types"
            + " to search."
    );
    private static final Set<String> VALIDATION_MESSAGES = Set.of(
        EventValidator.EVENT_NOT_SPECIFIED,
        EventValidator.SUMMARY_TOO_LONG,
        EventValidator.DESCRIPTION_TOO_LONG,
        ValidationMessages.EVENT_STATE_INVALID,
        ValidationMessages.CASE_DATA_INVALID,
        "No data provided",
        "Cannot create case because of event is not specified",
        "Cannot validate case field because of event is not specified"
    );

    private PublicValidationMessages() {
    }

    public static String messageFor(Exception exception, int status) {
        String message = exception.getMessage();
        if (message != null
            && ((status == 400 && exception instanceof BadRequestException && BAD_REQUEST_MESSAGES.contains(message))
            || (status == 422 && exception instanceof ValidationException && VALIDATION_MESSAGES.contains(message)))) {
            return message;
        }
        return HttpStatus.valueOf(status).getReasonPhrase();
    }
}
