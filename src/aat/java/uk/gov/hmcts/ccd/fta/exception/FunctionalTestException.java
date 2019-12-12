package uk.gov.hmcts.ccd.fta.exception;

public class FunctionalTestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FunctionalTestException(final String message) {
        super(message);
    }
}
