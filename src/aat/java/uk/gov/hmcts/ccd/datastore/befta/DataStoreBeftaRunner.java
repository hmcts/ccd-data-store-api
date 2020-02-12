package uk.gov.hmcts.ccd.datastore.befta;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;


@RunWith(Cucumber.class)
@CucumberOptions(plugin = "json:target/cucumber.json",
    glue = "uk.gov.hmcts.befta.player",
    features = {"classpath:features"})
public class DataStoreBeftaRunner extends BeftaMain {

    @BeforeClass
    public static void init() {
        setTaAdapter(new DefaultTestAutomationAdapter());
        BeftaTestDataLoader.main(null);
    }

}
