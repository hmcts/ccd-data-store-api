package uk.gov.hmcts.ccd.datastore.tests.helper;

import java.io.File;
import java.util.function.Supplier;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;

public class TestDataLoaderExtension extends BaseTest implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static final Logger LOG = LoggerFactory.getLogger(TestDataLoaderExtension.class);

    private static final String AUTO_TEST1_DEFINITION_FILE = "src/aat/resources/CCD_CNP_27_AUTOTEST1.xlsx";
    private static final String AUTO_TEST2_DEFINITION_FILE = "src/aat/resources/CCD_CNP_27_AUTOTEST2.xlsx";

    private static boolean testExecutionStarted = false;

    protected TestDataLoaderExtension() {
        super(AATHelper.INSTANCE);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!testExecutionStarted) {
            // one time setup code for all test classes extending this extension
            testExecutionStarted = true;
            LOG.info("Executing before all block for test data loader");
            loadData();
            // Registers the close method callback on completion of test execution of all tests running with this extension
            context.getRoot().getStore(GLOBAL).put(TestDataLoaderExtension.class, this);
        }
    }

    @Override
    public void close() {
        // tear down code goes here
    }

    protected void loadData() {
    }

    protected void importDefinitions() {
        importDefinition(AUTO_TEST1_DEFINITION_FILE);
        importDefinition(AUTO_TEST2_DEFINITION_FILE);
    }

    private void importDefinition(String file) {
        asAutoTestImporter()
            .given()
            .multiPart(new File(file))
            .expect()
            .statusCode(201)
            .when()
            .post("/import");
    }

    protected Long createCaseAndProgressState(Supplier<RequestSpecification> asUser, String caseType) {
        Long caseReference = createCase(asUser, caseType, AATCaseBuilder.EmptyCase.build());
        AATCaseType.Event.startProgress(caseType, caseReference)
            .as(asUser)
            .submit()
            .then()
            .statusCode(201)
            .assertThat()
            .body("state", Matchers.equalTo(AATCaseType.State.IN_PROGRESS));

        return caseReference;
    }

    protected Long createCase(Supplier<RequestSpecification> asUser, String caseType, AATCaseType.CaseData caseData) {
        return AATCaseType.Event.create(caseType)
            .as(asUser)
            .withData(caseData)
            .submitAndGetReference();
    }

    protected Long createCase(Supplier<RequestSpecification> asUser, String jurisdiction, String caseType, AATCaseType.CaseData caseData) {
        return AATCaseType.Event.create(jurisdiction, caseType)
            .as(asUser)
            .withData(caseData)
            .submitAndGetReference();
    }

}
