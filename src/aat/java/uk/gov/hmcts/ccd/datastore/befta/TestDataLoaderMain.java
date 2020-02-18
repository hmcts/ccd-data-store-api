package uk.gov.hmcts.ccd.datastore.befta;

public class TestDataLoaderMain {

    public static void main(String[] args) {
        new DataStoreTestAutomationAdapter().loadTestDataIfNecessary();
    }

}
