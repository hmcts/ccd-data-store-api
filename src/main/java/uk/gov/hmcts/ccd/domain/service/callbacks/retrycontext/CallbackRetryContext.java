package uk.gov.hmcts.ccd.domain.service.callbacks.retrycontext;

import lombok.ToString;

@ToString
public class CallbackRetryContext {
    private final Integer callbackRetryInterval;
    private final Integer callbackRetryTimeout;

    public CallbackRetryContext(final Integer callbackRetryInterval, final Integer callbackRetryTimeout) {
        this.callbackRetryInterval = callbackRetryInterval;
        this.callbackRetryTimeout = callbackRetryTimeout;
    }

    public Integer getCallbackRetryInterval() {
        return callbackRetryInterval;
    }

    public Integer getCallbackRetryTimeout() {
        return callbackRetryTimeout;
    }

}
