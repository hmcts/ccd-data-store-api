package uk.gov.hmcts.ccd.domain.service.callbacks.retrycontext;

import com.google.common.collect.Lists;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;

import java.util.List;

/**
 * This builder builds retries context based on the spreadsheet config for retries timeouts.
 * The intervals are fixed and are multiples of 3 starting from 0s and 1 i.e. 0s, 1s, 3s, 9s, 27s ...
 * Blank value means default retries (0s interval with 60s default timeout, 1s interval with 60s default timeout, 3s interval with 60s default timeout).
 * Single 0 value means disable retries (single send at 0s interval with 60s default timeout).
 * Any comma separated chain of values that DOES NOT include 0 means custom retries.
 * Any comma separated chain of values that DOES include 0 means default retries (see blank value).
 *
 * The examples input callback retry timeouts and output callback retry contexts are as follows:
 * blank - default retries context (0s interval with 60s default timeout, 1s interval with 60s default timeout, 3s interval with 60s default timeout)
 * 0 - disable retries (0s interval with 60s default callbacks timeout)
 * 1 - custom retries (0s interval with 1s custom timeout)
 * 1,5 - custom retries (0s interval with 1s custom timeout, 1s interval with 5s custom timeout)
 * 1,5,10 - custom retries (0s interval with 1s custom timeout, 1s interval with 5s custom timeout, 3s interval with 10s custom timeout)
 * 0,0,0,0 - default retries context (0s interval with 60s default timeout, 1s interval with 60s default timeout, 3s interval with 60s default timeout)
 * 0,5,0,10 - default retries context (0s interval with 60s default timeout, 1s interval with 60s default timeout, 3s interval with 60s default timeout)
 * 1,5,10,15 - custom retries (0s interval with 1s custom timeout, 1s interval with 5s custom timeout, 3s interval with 10s custom timeout, 9s interval with 15s custom timeout)
 * etc
 */
@Service
public class CallbackRetryContextBuilder {
    public static final int CALLBACK_RETRY_INTERVAL_MULTIPLIER = 3;
    private final List<Integer> defaultCallbackRetryIntervalsInSeconds;
    private final Integer defaultCallbackTimeoutInSeconds;

    public CallbackRetryContextBuilder(final ApplicationParams applicationParams) {
        this.defaultCallbackRetryIntervalsInSeconds = applicationParams.getCallbackRetryIntervalsInSeconds();
        this.defaultCallbackTimeoutInSeconds = applicationParams.getCallbackTimeoutInSeconds();
    }

    public List<CallbackRetryContext> buildCallbackRetryContexts(final List<Integer> callbackRetryTimeouts) {
        List<CallbackRetryContext> retryContextList = Lists.newArrayList();
        if (callbackRetryTimeouts.isEmpty() || isCallbackRetriesWithZeros(callbackRetryTimeouts)) {
            buildDefaultRetryContext(retryContextList);
        } else if (isCallbackRetriesDisabled(callbackRetryTimeouts)) {
            disableRetryContext(retryContextList);
        } else {
            buildCustomRetryContext(callbackRetryTimeouts, retryContextList);
        }
        return retryContextList;
    }

    private boolean isCallbackRetriesWithZeros(final List<Integer> callbackRetryTimeouts) {
        return callbackRetryTimeouts.size() != 1 && callbackRetryTimeouts.stream().anyMatch(callback -> callback == 0);
    }

    private void disableRetryContext(final List<CallbackRetryContext> retryContextList) {
        retryContextList.add(new CallbackRetryContext(0, defaultCallbackTimeoutInSeconds));
    }

    private void buildDefaultRetryContext(final List<CallbackRetryContext> retryContextList) {
        this.defaultCallbackRetryIntervalsInSeconds.forEach(cbRetryInterval -> retryContextList.add(
            new CallbackRetryContext(cbRetryInterval, defaultCallbackTimeoutInSeconds)));
    }

    private void buildCustomRetryContext(final List<Integer> callbackRetryTimeouts, final List<CallbackRetryContext> retryContextList) {
        retryContextList.add(new CallbackRetryContext(0, callbackRetryTimeouts.remove(0)));
        if (!callbackRetryTimeouts.isEmpty()) {
            retryContextList.add(new CallbackRetryContext(1, callbackRetryTimeouts.remove(0)));
            for (Integer callbackRetryTimeout : callbackRetryTimeouts) {
                retryContextList.add(
                    new CallbackRetryContext(
                        getLastElement(retryContextList).getCallbackRetryInterval() * CALLBACK_RETRY_INTERVAL_MULTIPLIER,
                        callbackRetryTimeout));
            }
        }
    }

    private CallbackRetryContext getLastElement(final List<CallbackRetryContext> retryContextList) {
        return retryContextList.get(retryContextList.size() - 1);
    }

    private boolean isCallbackRetriesDisabled(final List<Integer> callbackRetryTimeouts) {
        return callbackRetryTimeouts.size() == 1 && callbackRetryTimeouts.get(0) == 0;
    }
}
