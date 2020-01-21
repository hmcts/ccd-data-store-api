package uk.gov.hmcts.ccd.datastore.tests.helper;

import com.launchdarkly.client.LDClient;
import com.launchdarkly.client.LDUser;

public enum LDHelper {

    INSTANCE;

    private LDClient ldClient;
    private LDUser ldUser;

    LDHelper() {
        String sdkKey = System.getenv("LAUNCHDARKLY_SDK_KEY");
        String userKey = System.getenv("LAUNCHDARKLY_USER_KEY");
        String component = System.getenv("LAUNCHDARKLY_COMPONENT");

        ldClient = new LDClient(sdkKey);
        ldUser = new LDUser.Builder(userKey)
            .custom("component", component)
            .build();
    }

    public LDClient getClient() {
        return ldClient;
    }

    public LDUser getUser() {
        return ldUser;
    }
}
