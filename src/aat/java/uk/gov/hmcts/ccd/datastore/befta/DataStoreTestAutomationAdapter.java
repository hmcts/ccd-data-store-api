package uk.gov.hmcts.ccd.datastore.befta;

import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.TestDataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.befta.util.ReflectionUtils;

public class DataStoreTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private TestDataLoaderToDefinitionStore loader = new TestDataLoaderToDefinitionStore(this);

    @Override
    public void doLoadTestData() {
        loader.addCcdRoles();
        loader.importDefinitions();
    }

    @Override
    public Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        String docAmUrl = EnvironmentVariableUtils.getRequiredVariable("CASE_DOCUMENT_AM_URL");
        if (key.equals("documentIdInTheResponse")) {
            try {
                String href = (String) ReflectionUtils
                    .deepGetFieldInObject(scenarioContext,
                                          "testData.actualResponse.body._embedded.documents[0]._links.self.href");
                return href.substring(href.length() - 36);
            } catch (Exception exception) {
//                logger.error("Exception while getting the Document ID from the response :{}", exception.getMessage());
                return "Error extracting the Document Id";
            }
        } else if (key.toString().equalsIgnoreCase("validSelfLink")) {
            try {
                String self = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                                                                            "testData.actualResponse.body._embedded.documents[0]._links.self.href");
                scenarioContext.getScenario().write("Self: " + self);
                if (self != null && self.startsWith(docAmUrl + "/cases/documents/")) {
                    return self;
                }
                return docAmUrl + "/cases/documents/<a document id>";
            } catch (Exception e) {
                throw new FunctionalTestException("Couldn't get self link from response field", e);
            }

        } else if (key.toString().equalsIgnoreCase("validBinaryLink")) {
            try {
                String binary = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                                                                              "testData.actualResponse.body._embedded.documents[0]._links.binary.href");
                scenarioContext.getScenario().write("Binary: " + binary);
                if (binary != null && binary.startsWith(docAmUrl + "/cases/documents/") && binary.endsWith("/binary")) {
                    return binary;
                }
                return docAmUrl + "/cases/documents/<a document id>/binary";
            } catch (Exception e) {
                throw new FunctionalTestException("Couldn't get binary link from response field", e);
            }
        }
        return super.calculateCustomValue(scenarioContext, key);
    }
}
