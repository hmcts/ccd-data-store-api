package uk.gov.hmcts.ccd.datastore.tests.helper;

import com.launchdarkly.client.LDClient;

public enum LDHelper {

    INSTANCE;

    private LDClient ldClient;

    LDHelper() {
        String sdkKey = System.getenv("LAUNCHDARKLY_SDK_KEY");
        ldClient = new LDClient(sdkKey);
    }

    public LDClient getClient() {
        return ldClient;
    }
}
