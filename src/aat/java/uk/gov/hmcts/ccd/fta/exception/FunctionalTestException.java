package uk.gov.hmcts.ccd.fta.exception;

public class FunctionalTestException extends RuntimeException {

    private final String validationMessage;

    public FunctionalTestException(final String message) {
        super(message);
        validationMessage = message;
    }

    public String getValidationMessage() {
        return validationMessage;
    }
}
