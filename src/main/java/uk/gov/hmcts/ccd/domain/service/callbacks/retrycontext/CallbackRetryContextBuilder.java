package uk.gov.hmcts.ccd.domain.service.callbacks.retrycontext;

import com.google.common.collect.Lists;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;

import java.util.List;

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
