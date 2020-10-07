package uk.gov.hmcts.ccd.appinsights;

public class CallbackTelemetryContext {

    private final String callbackType;

    public CallbackTelemetryContext(String callbackType) {
        this.callbackType = callbackType;
    }

    public String getCallbackType() {
        return callbackType;
    }
}
