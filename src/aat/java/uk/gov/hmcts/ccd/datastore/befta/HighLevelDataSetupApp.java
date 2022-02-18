package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.dse.ccd.CcdEnvironment;
import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;

import java.util.Locale;

public class HighLevelDataSetupApp extends DataLoaderToDefinitionStore {

    public HighLevelDataSetupApp(CcdEnvironment dataSetupEnvironment) {
        super(dataSetupEnvironment);
    }

    public static void main(String[] args) throws Throwable {
        if (!args[0].toLowerCase(Locale.ENGLISH).equals("prod")) {
            main(HighLevelDataSetupApp.class, args);
        }
    }

    @Override
    protected boolean shouldTolerateDataSetupFailure() {
        return true;
    }

}
