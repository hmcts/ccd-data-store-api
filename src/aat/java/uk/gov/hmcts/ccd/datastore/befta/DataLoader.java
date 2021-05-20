package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;

public class DataLoader extends DataLoaderToDefinitionStore {

    public DataLoader(String dataSetupEnvironment) {
        super(dataSetupEnvironment);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("args shouldn't be empty");
        }

        DataLoader loader = new DataLoader(args[0]);
        loader.addCcdRoles();
        loader.importDefinitionsAt(VALID_CCD_TEST_DEFINITIONS_PATH);
    }
}
