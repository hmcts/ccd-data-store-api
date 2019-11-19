package uk.gov.hmcts.ccd.fta.util;

import org.junit.Assert;
import uk.gov.hmcts.ccd.datastore.tests.Env;

public class EnvUtils {

    public static String resolvePossibleEnvironmentVariable(String key) {
        if (key.startsWith("[[$")) {
            String envKey = key.substring(3, key.length() - 2);
            String envValue = Env.require(envKey);
            String errorMessage = "Specified environment variable '" + envValue + "' not found";
            Assert.assertNotNull(errorMessage, envValue);
            return envValue;
        }
        return key;
    }
}
