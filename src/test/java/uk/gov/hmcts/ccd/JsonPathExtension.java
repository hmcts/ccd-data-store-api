package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import uk.gov.hmcts.ccd.config.JaywayJsonPathConfigHelper;

public class JsonPathExtension implements BeforeAllCallback {

    private static boolean testExecutionStarted = false;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!testExecutionStarted) {
            // one time setup code for all test classes extending this extension
            JaywayJsonPathConfigHelper.configureJsonPathForJackson();
            testExecutionStarted = true;
        }
    }

}
