package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.BeftaMain;

public class DataStoreBeftaMain {

    private DataStoreBeftaMain() {
      // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    }

    public static void main(String[] args) {
        BeftaMain.main(args, new DataStoreTestAutomationAdapter());
    }

}
