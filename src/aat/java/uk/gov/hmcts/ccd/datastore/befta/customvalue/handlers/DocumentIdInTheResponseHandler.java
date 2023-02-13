package uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers;

import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey;

import static uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey.DOCUMENT_ID_IN_RESPONSE;

public class DocumentIdInTheResponseHandler implements CustomValueHandler {

    @Override
    public Boolean matches(CustomValueKey key) {
        return DOCUMENT_ID_IN_RESPONSE.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {
            String href = (String) ReflectionUtils
                .deepGetFieldInObject(scenarioContext,
                    "testData.actualResponse.body.documents[0]._links.self.href");
            return href.substring(href.length() - 36);
        } catch (Exception exception) {
            return "Error extracting the Document Id";
        }
    }

}
