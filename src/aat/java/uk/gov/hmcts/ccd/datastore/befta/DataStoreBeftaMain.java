package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.BeftaMain;

public class DataStoreBeftaMain {

    public static void main(String[] args) {
        BeftaMain.main(args, new DataStoreTestAutomationAdapter());
    }

}
