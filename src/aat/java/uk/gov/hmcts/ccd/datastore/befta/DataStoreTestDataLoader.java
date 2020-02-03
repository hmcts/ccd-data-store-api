package uk.gov.hmcts.ccd.datastore.befta;

public class DataStoreTestDataLoader {

    public static void main(String[] args) {
        new DataStoreTestAutomationAdapter().loadTestDataIfNecessary();
    }

}
