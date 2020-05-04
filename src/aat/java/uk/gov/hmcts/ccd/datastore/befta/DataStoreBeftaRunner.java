package uk.gov.hmcts.ccd.datastore.befta;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import uk.gov.hmcts.befta.BeftaMain;


@RunWith(Cucumber.class)
@CucumberOptions(plugin = "json:target/cucumber.json",
    glue = "uk.gov.hmcts.befta.player",
        features = { "classpath:features" }, tags = { "not @Ignore" })
public class DataStoreBeftaRunner {

    @BeforeClass
    public static void setUp() {
        BeftaMain.setUp(new DataStoreTestAutomationAdapter());
    }

    @AfterClass
    public static void tearDown() {
        BeftaMain.tearDown();
    }

}
