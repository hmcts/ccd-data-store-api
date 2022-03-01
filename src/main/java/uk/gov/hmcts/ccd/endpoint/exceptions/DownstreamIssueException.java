package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_GATEWAY)
public class DownstreamIssueException extends RuntimeException {
    public DownstreamIssueException(final String message) {
        super(message);
    }
}
