package uk.gov.hmcts.ccd.datastore.befta;

public class TestDataLoaderMain {

    private TestDataLoaderMain() {
      // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    }

    public static void main(String[] args) {
        System.out.println("Args:"+ args[0]);
        new DataStoreTestAutomationAdapter().getDataLoader().loadTestDataIfNecessary();
    }

}
