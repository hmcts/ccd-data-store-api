package uk.gov.hmcts.ccd.domain.service.callbacks;

public enum CallbackType {

    ABOUT_TO_START("AboutToStart"),
    ABOUT_TO_SUBMIT("AboutToSubmit"),
    SUBMITTED("Submitted"),
    MID_EVENT("MidEvent");

    private String value;

    CallbackType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}


