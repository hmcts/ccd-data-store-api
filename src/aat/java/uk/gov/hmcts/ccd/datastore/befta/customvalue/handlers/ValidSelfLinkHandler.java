package uk.gov.hmcts.ccd.datastore.befta.customvalue.handlers;

import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.BeftaUtils;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.befta.util.ReflectionUtils;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueHandler;
import uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey;

import static uk.gov.hmcts.ccd.datastore.befta.customvalue.CustomValueKey.VALID_SELF_LINK;

public class ValidSelfLinkHandler implements CustomValueHandler {

    private final String docAmUrl = EnvironmentVariableUtils.getRequiredVariable("CASE_DOCUMENT_AM_URL");

    @Override
    public Boolean matches(CustomValueKey key) {
        return VALID_SELF_LINK.equals(key);
    }

    @Override
    public Object calculate(BackEndFunctionalTestScenarioContext scenarioContext, Object key) {
        try {
            String self = (String) ReflectionUtils.deepGetFieldInObject(scenarioContext,
                "testData.actualResponse.body.documents[0]._links.self.href");
            BeftaUtils.defaultLog("Self: " + self);
            if (self != null && self.startsWith(docAmUrl + "/cases/documents/")) {
                return self;
            }
            return docAmUrl + "/cases/documents/<a document id>";
        } catch (Exception e) {
            throw new FunctionalTestException("Couldn't get self link from response field", e);
        }
    }

}
