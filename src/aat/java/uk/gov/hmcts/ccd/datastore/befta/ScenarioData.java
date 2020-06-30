package uk.gov.hmcts.ccd.datastore.befta;

public class ScenarioData {


    private static String uniqueString;

    public static String getUniqueString() {
        return uniqueString;
    }

    public static void setUniqueString(String uniqueString) {
        ScenarioData.uniqueString = uniqueString;
    }

}
