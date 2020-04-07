package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.TestDataLoaderToDefinitionStore;

public class DataStoreTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private TestDataLoaderToDefinitionStore loader = new TestDataLoaderToDefinitionStore(this);

    @Override
    public void doLoadTestData() {
        loader.addCcdRoles();
        loader.importDefinitions();
    }

}
