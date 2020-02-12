package uk.gov.hmcts.ccd.datastore.befta;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import uk.gov.hmcts.befta.BeftaMain;


@RunWith(Cucumber.class)
@CucumberOptions(plugin = "json:target/cucumber.json",
    glue = "uk.gov.hmcts.befta.player",
    features = {"classpath:features"})
public class DataStoreBeftaRunner {

    @BeforeClass
    public static void init() {
        BeftaMain.setUp();
    }

}
