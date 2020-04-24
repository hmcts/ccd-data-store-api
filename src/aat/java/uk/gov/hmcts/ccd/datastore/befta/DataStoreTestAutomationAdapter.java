package uk.gov.hmcts.ccd.documentam.befta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.TestDataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.befta.util.ReflectionUtils;

public class DataStoreTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DataStoreTestAutomationAdapter.class);

    private TestDataLoaderToDefinitionStore loader = new TestDataLoaderToDefinitionStore(this);

    @Override
    public void doLoadTestData() {
        loader.addCcdRoles();
        loader.importDefinitions();
        super.registerApiClientWithEnvVariable("API_CLIENT_DATA_STORE");
    }

    @Override
    public Object calculateCustomValue(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        String docAmUrl = EnvironmentVariableUtils.getRequiredVariable("CASE_DOCUMENT_AM_URL");
        switch (key.toString()) {
            case ("documentIdInTheResponse"):
                return getDocumentIdInTheRresponse(scenarioContext);
            case ("validSelfLink"):
                return getValidSelfLink(scenarioContext, docAmUrl);
            case ("validBinaryLink"):
                return getValidBinaryLink(scenarioContext, docAmUrl);
            case ("hashTokenDifferentFromPrevious"):
                return getHashTokenDifferentFromPrevious(scenarioContext);
            default:
                return super.calculateCustomValue(scenarioContext, key);
        }

    }

    private Object getHashTokenDifferentFromPrevious(BackEndFunctionalTestScenarioContext scenarioContext) {
        try {
            String newHashToken = (String) ReflectionUtils
                .deepGetFieldInObject(scenarioContext,
                    "testData.actualResponse.body.hashToken");

            String previousHashToken = (String) ReflectionUtils
                .deepGetFieldInObject(scenarioContext,
                    "childContexts.S-064_Get_Hash_Token.testData.actualResponse.body.hashToken");

            scenarioContext.getScenario().write("previousHashToken: " + previousHashToken);
            if (newHashToken != null && !newHashToken.equalsIgnoreCase(previousHashToken)) {
                return newHashToken;
            }
            return previousHashToken;
        } catch (Exception e) {
            throw new FunctionalTestException("Couldn't get previousHashToken from child context response field", e);
        }
    }

    private Object getValidBinaryLink(BackEndFunctionalTestScenarioContext scenarioContext, String docAmUrl) {
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

    private Object getValidSelfLink(BackEndFunctionalTestScenarioContext scenarioContext, String docAmUrl) {
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
    }

    private Object getDocumentIdInTheRresponse(BackEndFunctionalTestScenarioContext scenarioContext) {
        try {
            String href = (String) ReflectionUtils
                .deepGetFieldInObject(scenarioContext,
                    "testData.actualResponse.body._embedded.documents[0]._links.self.href");
            return href.substring(href.length() - 36);
        } catch (Exception exception) {
            logger.error("Exception while getting the Document ID from the response :{}", exception.getMessage());
            return "Error extracting the Document Id";
        }
    }
}
