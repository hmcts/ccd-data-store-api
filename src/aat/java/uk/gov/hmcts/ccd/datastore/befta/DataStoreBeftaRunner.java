package uk.gov.hmcts.ccd.datastore.befta;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.ccd.datastore.tests.helper.elastic.ElasticsearchTestDataLoaderExtension;


@RunWith(Cucumber.class)
@CucumberOptions(plugin = "json:target/cucumber.json",
    glue = {"uk.gov.hmcts.befta.player", "uk.gov.hmcts.ccd.datastore.befta"}, features = { "classpath:features" }, tags = { "not @Ignore" })
public class DataStoreBeftaRunner {

    private DataStoreBeftaRunner() {
        // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    }

    @BeforeClass
    public static void setUp() {
        BeftaMain.setUp(new DataStoreTestAutomationAdapter());
    }

    @AfterClass
    public static void tearDown() {
        new ElasticsearchTestDataLoaderExtension().close();
    }

}
