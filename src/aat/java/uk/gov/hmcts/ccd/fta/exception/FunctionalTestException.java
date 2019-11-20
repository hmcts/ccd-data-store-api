package uk.gov.hmcts.ccd.fta.exception;

public class FunctionalTestException extends RuntimeException {
    public FunctionalTestException(final String message) {
        super(message);
    }
}
