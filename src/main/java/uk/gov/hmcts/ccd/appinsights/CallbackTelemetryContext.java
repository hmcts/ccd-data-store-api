package uk.gov.hmcts.ccd.appinsights;

import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType;

public class CallbackTelemetryContext {

    private final CallbackType callbackType;

    public CallbackTelemetryContext(CallbackType callbackType) {
        this.callbackType = callbackType;
    }

    public CallbackType getCallbackType() {
        return callbackType;
    }
}
