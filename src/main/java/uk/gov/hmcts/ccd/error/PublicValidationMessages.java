package uk.gov.hmcts.ccd.error;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.ccd.domain.model.std.validator.EventValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Set;

/** Explicitly reviewed public messages; dynamic exception diagnostics remain private. */
public final class PublicValidationMessages {
    private static final Set<String> BAD_REQUEST_MESSAGES = Set.of(
        "Case reference is not valid",
        "Case ID is not valid",
        "Case Type Id is not valid",
        "Missing start trigger token",
        "Start trigger token has expired. Please restart the event.",
        "Access can only be 'create', 'read' or 'update'",
        "Sort order field is invalid.",
        "003 Wrong CaseVersion",
        "002 Invalid categoryId"
    );
    private static final Set<String> VALIDATION_MESSAGES = Set.of(
        EventValidator.EVENT_NOT_SPECIFIED,
        EventValidator.SUMMARY_TOO_LONG,
        EventValidator.DESCRIPTION_TOO_LONG,
        "The case status did not qualify for the event",
        "Case data validation failed"
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
