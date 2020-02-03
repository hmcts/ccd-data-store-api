package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.BeftaMain;

public class DataStoreBeftaMain extends BeftaMain {

    public static void main(String[] args) {
        setTaAdapter(new DataStoreTestAutomationAdapter());
        BeftaMain.main(args);
    }

}
