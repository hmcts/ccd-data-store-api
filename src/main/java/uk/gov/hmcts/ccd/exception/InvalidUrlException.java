package uk.gov.hmcts.ccd.exception;

public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String message, Throwable cause) {
        super(message, cause);
    }

}
