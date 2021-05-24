package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;

public class DataLoader extends DataLoaderToDefinitionStore {
    public DataLoader() {
        super();
    }
    public static void main(String[] args) {
        DataLoader loader = new DataLoader();
        loader.addCcdRoles();
        loader.importDefinitionsAt(VALID_CCD_TEST_DEFINITIONS_PATH);
    }
}
