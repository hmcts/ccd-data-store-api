package uk.gov.hmcts.ccd.datastore.tests.helper;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;

public class CaseTestDataLoaderExtension extends BaseTest implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static final Logger LOG = LoggerFactory.getLogger(CaseTestDataLoaderExtension.class);

    private static boolean testExecutionStarted = false;

    protected CaseTestDataLoaderExtension() {
        super(AATHelper.INSTANCE);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!testExecutionStarted) {
            // one time setup code for all test classes extending this extension
            testExecutionStarted = true;
            LOG.info("Executing before all block for test data loader : CaseTestDataLoaderExtension");
            // Registers the close method callback on completion of test execution of all tests running with this extension
            context.getRoot().getStore(GLOBAL).put(CaseTestDataLoaderExtension.class, this);
        }
    }

    @Override
    public void close() {
        // tear down code goes here
    }

}
