package uk.gov.hmcts.ccd.datastore.tests.befta;

import uk.gov.hmcts.befta.TestAutomationConfig;
import uk.gov.hmcts.befta.player.DefaultBackEndFunctionalTestScenarioPlayer;

public class DataStoreApiBackEndFunctionalTestScenarioPlayer extends DefaultBackEndFunctionalTestScenarioPlayer {

    public DataStoreApiBackEndFunctionalTestScenarioPlayer() {
        super(new DataStoreTestAutomationAdapter(TestAutomationConfig.INSTANCE));
    }

}
