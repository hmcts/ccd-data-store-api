package uk.gov.hmcts.ccd.data.draft;

public class DraftAccessException extends RuntimeException {

    public DraftAccessException(String message) {
        super(message);
    }

    public DraftAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
