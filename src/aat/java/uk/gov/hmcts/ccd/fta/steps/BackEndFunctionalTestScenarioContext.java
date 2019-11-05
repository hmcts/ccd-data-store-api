package uk.gov.hmcts.ccd.fta.steps;

import org.junit.Assert;

import uk.gov.hmcts.ccd.fta.data.HttpTestData;
import uk.gov.hmcts.ccd.fta.data.HttpTestDataSource;
import uk.gov.hmcts.ccd.fta.data.JsonStoreHttpTestDataSource;

public class BackEndFunctionalTestScenarioContext {

    private static final String[] TEST_DATA_RESOURCE_PACKAGES = { "features" };
    private static final HttpTestDataSource DATA_SOURCE = new JsonStoreHttpTestDataSource(TEST_DATA_RESOURCE_PACKAGES);

    public void loadTestData(String scenarioKey) {
        HttpTestData testData = DATA_SOURCE.getDataForScenario(scenarioKey);
        Assert.assertEquals("S-129", testData.get_guid_());
    }
}
