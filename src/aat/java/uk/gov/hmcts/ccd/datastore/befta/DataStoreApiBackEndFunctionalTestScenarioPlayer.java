package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.TestAutomationConfig;
import uk.gov.hmcts.befta.player.DefaultBackEndFunctionalTestScenarioPlayer;

public class DataStoreApiBackEndFunctionalTestScenarioPlayer extends DefaultBackEndFunctionalTestScenarioPlayer {

    public DataStoreApiBackEndFunctionalTestScenarioPlayer() {
        super(new DataStoreTestAutomationAdapter(TestAutomationConfig.INSTANCE));
    }

}
