package uk.gov.hmcts.ccd.fta.steps;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.fta.data.HttpTestData;
import uk.gov.hmcts.ccd.fta.data.HttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.JsonStoreHttpTestDataSource;

public class DynamicValueInjectorTest {

    private static final String[] TEST_DATA_RESOURCE_PACKAGES = { "framework-test-data" };
    private static final HttpTestDataSource TEST_DATA_RESOURCE = new JsonStoreHttpTestDataSource(
            TEST_DATA_RESOURCE_PACKAGES);

    private BackEndFunctionalTestScenarioContext scenarioContext;

    @Mock
    private AATHelper aat;

    @Before
    public void prepareScenarioConext() {
        scenarioContext = new BackEndFunctionalTestScenarioContextForTest();

        scenarioContext.initializeTestDataFor("Simple-Test-Data-With-All-Possible-Dynamic-Values");
        
        BackEndFunctionalTestScenarioContext subcontext = new BackEndFunctionalTestScenarioContextForTest();
        subcontext.initializeTestDataFor("Token_Creation_Call");
        subcontext.getTestData().setActualResponse(subcontext.getTestData().getExpectedResponse());

        scenarioContext.setTheInvokingUser(scenarioContext.getTestData().getInvokingUser());
        scenarioContext.addChildContext(subcontext);
    }
    
    @Test
    public void shoudlInjectAllValues() {

        HttpTestData testData = scenarioContext.getTestData();

        DynamicValueInjector underTest = new DynamicValueInjector(aat, testData, scenarioContext);

        Assert.assertEquals("[[DYNAMIC]]", testData.getRequest().getPathVariables().get("uid"));

        underTest.injectDataFromContext();

        Assert.assertEquals("mutlu.sancaktutar@hmcts.net", testData.getRequest().getPathVariables().get("email"));

        Assert.assertEquals("token value", testData.getRequest().getPathVariables().get("token"));
        Assert.assertEquals("token value at index 2", testData.getRequest().getPathVariables().get("token_2"));

        // Assert.assertEquals("[[DYNAMIC]]",
        // testData.getRequest().getHeaders().get("uid"));

    }

    class BackEndFunctionalTestScenarioContextForTest extends BackEndFunctionalTestScenarioContext {

        @Override
        public void initializeTestDataFor(String testDataId) {
            testData = TEST_DATA_RESOURCE.getDataForTestCall(testDataId);
        }
    }
}

