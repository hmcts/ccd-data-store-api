package uk.gov.hmcts.ccd.datastore.befta;

public class ScenarioData {

    private ScenarioData() {
        // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    }

    private static String uniqueString;

    public static String getUniqueString() {
        return uniqueString;
    }

    public static void setUniqueString(String uniqueString) {
        ScenarioData.uniqueString = uniqueString;
    }

}
