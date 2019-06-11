package uk.gov.hmcts.ccd.datastore.tests.util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.with;

public class TestUtils {

    public static void withRetries(long retryPollDelay, long retryPollInterval, String conditionDesc, Callable<Boolean> conditionEvaluator) {
        with()
            .pollDelay(retryPollDelay, TimeUnit.MILLISECONDS)
            .and()
            .pollInterval(retryPollInterval, TimeUnit.MILLISECONDS)
            .await(conditionDesc)
            .atMost(60, TimeUnit.SECONDS)
            .until(conditionEvaluator);
    }

}
