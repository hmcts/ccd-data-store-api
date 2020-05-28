package uk.gov.hmcts.ccd.datastore.befta;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import uk.gov.hmcts.befta.BeftaMain;

public class DataStoreBeftaMain {

    private DataStoreBeftaMain() {
        // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    }

    public static void main(String[] args) {
        BeftaMain.main(args, new DataStoreTestAutomationAdapter());
    }

    @Tag("smoke")
    @Test
    public void shouldRetrieveWhenExists() {

    }

}
