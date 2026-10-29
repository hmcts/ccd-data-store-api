package uk.gov.hmcts.ccd.domain.model.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.io.Serializable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpErrorRedactionTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "Case reference is not valid", "Case ID is not valid", "Case Type Id is not valid",
        "Missing start trigger token", "Start trigger token has expired. Please restart the event.",
        "Access can only be 'create', 'read' or 'update'", "Sort order field is invalid.",
        "003 Wrong CaseVersion", "002 Invalid categoryId"
    })
    void preservesApprovedBadInputMessages(String message) {
        BadRequestException exception = new BadRequestException(message);
        assertThat(new HttpError<>(exception, new MockHttpServletRequest()).getMessage()).isEqualTo(message);
        assertThat(new HttpError<>(exception, "/case", HttpStatus.BAD_REQUEST).getMessage()).isEqualTo(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Cannot create event because event is not specified",
        "An event summary must be less than 1024 characters",
        "An event description must be less than 65536 characters",
        "The case status did not qualify for the event", "Case data validation failed"
    })
    void preservesApprovedValidationMessages(String message) {
        ValidationException exception = new ValidationException(message);
        assertThat(new HttpError<>(exception, new MockHttpServletRequest()).getMessage()).isEqualTo(message);
        assertThat(new HttpError<>(exception, "/case", HttpStatus.UNPROCESSABLE_ENTITY).getMessage())
            .isEqualTo(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SECRET jdbc://internal", "Case reference is not valid SECRET",
        "003 Wrong CaseVersion SECRET", "002 Invalid categoryId SECRET", ""
    })
    void redactsUnapprovedApiMessages(String message) {
        assertThat(new HttpError<>(new BadRequestException(message), new MockHttpServletRequest()).getMessage())
            .isEqualTo("Bad Request");
        assertThat(new HttpError<>(new ValidationException(message), new MockHttpServletRequest()).getMessage())
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase());
    }

    @Test
    void keepsUnexpectedFailuresGenericEvenWithAnApprovedMessage() {
        assertThat(new HttpError<>(new RuntimeException("Case reference is not valid"), "/case",
            HttpStatus.BAD_REQUEST).getMessage()).isEqualTo("Bad Request");
        assertThat(new HttpError<>(new BadRequestException("Case reference is not valid"), "/case",
            HttpStatus.INTERNAL_SERVER_ERROR).getMessage()).isEqualTo("Internal Server Error");
        assertThat(new HttpError<>(new BadRequestException(null), new MockHttpServletRequest()).getMessage())
            .isEqualTo("Bad Request");
    }

    @Test
    void serialisationPreservesValidationDetailsAndCallbackFeedback() throws Exception {
        HttpError<Serializable> error = new HttpError<Serializable>(
            new ValidationException("Case data validation failed"), "/case", HttpStatus.UNPROCESSABLE_ENTITY)
            .withDetails(new String[]{"TextField is required"});
        error.withCallbackErrors(List.of("Correct the date"));
        error.withCallbackWarnings(List.of("Check the address"));
        var json = new ObjectMapper().findAndRegisterModules().valueToTree(error);
        assertThat(json.get("message").asText()).isEqualTo("Case data validation failed");
        assertThat(json.get("details").get(0).asText()).isEqualTo("TextField is required");
        assertThat(json.get("callbackErrors").get(0).asText()).isEqualTo("Correct the date");
        assertThat(json.get("callbackWarnings").get(0).asText()).isEqualTo("Check the address");
    }

    @Test
    void serialisedErrorOmitsExceptionPathAndCause() throws Exception {
        RuntimeException exception = new RuntimeException("SECRET jdbc://internal",
            new IllegalStateException("SECRET database cause"));
        HttpError<Serializable> error = new HttpError<Serializable>(exception, "/private-case-123",
            HttpStatus.BAD_REQUEST).withDetails(exception.getCause());
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String json = mapper.writeValueAsString(error);
        assertThat(json).contains("INVALID_REQUEST", "Bad Request")
            .doesNotContain("SECRET", "jdbc", "RuntimeException", "private-case", "cause", "exception", "path");
        assertThat(mapper.readTree(json).get("status").asInt()).isEqualTo(400);
    }
}
